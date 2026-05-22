package com.cryptodept.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.data.datastore.SubscriptionAccessManager
import com.cryptodept.data.datastore.SystemSettingsManager
import com.cryptodept.data.remoteconfig.RemoteConfigService
import com.cryptodept.domain.model.*
import com.cryptodept.domain.usecase.*
import com.cryptodept.domain.manager.DashboardLogService
import com.cryptodept.domain.tier.AccessTier
import com.cryptodept.domain.tier.FeatureKey
import com.cryptodept.domain.tier.TierAccessManager
import com.cryptodept.domain.usecase.prediction.GetDailyAIPickUseCase
import com.cryptodept.domain.usecase.prediction.DailyAIPick
import com.cryptodept.domain.usecase.whale.GetWhaleInsightUseCase
import com.cryptodept.domain.model.WhaleSignal
import com.cryptodept.util.AnalyticsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val observeTickerUseCase: ObserveTickerUseCase,
    private val getNetworkHealthUseCase: GetNetworkHealthUseCase,
    private val refreshPricesUseCase: RefreshPricesUseCase,
    private val aiGenerator: AIReportGenerator,
    private val riskEngine: RiskScoreEngine,
    private val logService: DashboardLogService,
    private val analytics: AnalyticsService,
    private val settings: SystemSettingsManager,
    private val subscription: SubscriptionAccessManager,
    private val agentCoordinator: MultiAgentCoordinator,
    private val demoMode: com.cryptodept.util.DemoModeProvider,
    private val remoteConfig: RemoteConfigService,
    private val tierAccessManager: TierAccessManager,
    private val firebaseDataSource: com.cryptodept.data.remote.source.FirebaseRemoteDataSource,
    private val getWhaleInsightUseCase: GetWhaleInsightUseCase,
    private val getDailyAIPickUseCase: GetDailyAIPickUseCase,
    private val getMacroIntelligenceUseCase: GetMacroIntelligenceUseCase,
    private val getOHLCUseCase: GetOHLCUseCase,
    private val refreshOHLCUseCase: RefreshOHLCUseCase,
    private val getLiquidationSummaryUseCase: GetLiquidationSummaryUseCase,
    private val integrityService: com.cryptodept.domain.manager.SystemIntegrityService,
) : ViewModel() {

    // ============================================================
    // CLOUD INTELLIGENCE FLOW
    // ============================================================
    val cloudState: StateFlow<com.cryptodept.data.remote.model.CloudTerminalState?> = 
        firebaseDataSource.getTerminalState()
            .onEach { triggerScanLine() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ============================================================
    // MACRO INTELLIGENCE FLOWS
    // ============================================================
    private val _macroIntelligence = MutableStateFlow<MacroIntelligence?>(null)
    val macroIntelligence: StateFlow<MacroIntelligence?> = _macroIntelligence.combine(cloudState) { local, cloud ->
        cloud?.macroBriefing?.let { b ->
            MacroIntelligence(
                btcDominance = b.btcDominance,
                btcDominanceDelta24h = 0.0,
                ethGasGwei = b.ethGasGwei,
                globalMarketCapUsd = b.globalMarketCapUsd,
                altcoinSeasonIndex = b.altcoinSeasonIndex,
                globalLiquidityUsd = b.globalLiquidityUsd,
                gasPrediction = b.gasPrediction, // NEW: M1.2
                totalLiquidations1h = LiquidationSnapshot(b.liquidations1h.totalUsd, b.liquidations1h.longsUsd, b.liquidations1h.shortsUsd, cloud.lastUpdateTimestamp),
                totalLiquidations24h = LiquidationSnapshot(b.liquidations24h.totalUsd, b.liquidations24h.longsUsd, b.liquidations24h.shortsUsd, cloud.lastUpdateTimestamp)
            )
        } ?: local
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ============================================================
    // TIER ACCESS FLOWS
    // ============================================================
    val currentTier: StateFlow<AccessTier> = tierAccessManager.currentTier

    val canSeeFullAINarrative: Flow<Boolean> = 
        tierAccessManager.hasAccessFlow(FeatureKey.DASHBOARD_AI_NARRATIVE_FULL)

    val canSeeLiveWhaleFeed: Flow<Boolean> = 
        tierAccessManager.hasAccessFlow(FeatureKey.DASHBOARD_WHALE_FEED_LIVE)

    val canSeeSentimentMatrix: Flow<Boolean> = 
        tierAccessManager.hasAccessFlow(FeatureKey.DASHBOARD_SENTIMENT_MATRIX)


    val focusModeEnabled: StateFlow<Boolean> = settings.focusModeEnabled.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false,
    )

    val events = logService.events.combine(demoMode.demoActiveState) { realEvents, active ->
        if (active) demoMode.getDemoEvents() else realEvents
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _selectedAgentId = MutableStateFlow("SENTINEL")
    val selectedAgentId: StateFlow<String> = _selectedAgentId.asStateFlow()

    private val _localAgentReports = MutableStateFlow<Map<String, String>>(emptyMap())

    private val _aiSummary = MutableStateFlow("ANALYZING MARKET DYNAMICS...")
    val aiSummary: StateFlow<String> = combine(_aiSummary, cloudState, demoMode.demoActiveState, selectedAgentId, _localAgentReports) { localSummary, cloud, active, agentId, localReports ->
        if (active) {
            demoMode.getDemoAiNarrative()
        } else if (cloud != null && !cloud.agentReports[agentId].isNullOrBlank()) {
            // ALWAYS use cloud report if available, ignore 10min freshness for narrative
            cloud.agentReports[agentId]!!
        } else {
            localReports[agentId] ?: localSummary
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "ANALYZING MARKET DYNAMICS...")

    fun selectAgent(agentId: String) {
        _selectedAgentId.value = agentId
        analytics.logEvent("agent_switched", android.os.Bundle().apply { putString("agent_id", agentId) })
        
        // Trigger local generation if cloud report for this agent is missing
        val cloud = cloudState.value
        val now = System.currentTimeMillis()
        val isCloudFresh = cloud != null && (now - cloud.lastUpdateTimestamp) < 600_000
        if (!isCloudFresh || cloud?.agentReports?.get(agentId).isNullOrBlank()) {
            fetchAiSummary(networkHealth.value ?: NetworkHealth("N/A", "N/A", "N/A", 50, "Neutral"), agentId)
        }
    }

    private val _isAiStreaming = MutableStateFlow(false)
    val isAiStreaming: StateFlow<Boolean> = _isAiStreaming.combine(demoMode.demoActiveState) { realStreaming, active ->
        if (active) false else realStreaming
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _agentStatuses = MutableStateFlow<Map<String, AgentStatus>>(emptyMap())
    val agentStatuses: StateFlow<Map<String, AgentStatus>> = combine(_agentStatuses, cloudState, demoMode.demoActiveState) { local, cloud, active ->
        if (active) demoMode.getDemoAgentStatuses()
        else if (!cloud?.agentStatuses.isNullOrEmpty()) {
            cloud!!.agentStatuses.mapValues { entry ->
                try { AgentStatus.valueOf(entry.value) } catch (e: Exception) { AgentStatus.READY }
            }
        }
        else local
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private val _networkHealth = MutableStateFlow<NetworkHealth?>(null)
    @OptIn(ExperimentalCoroutinesApi::class)
    val networkHealth: StateFlow<NetworkHealth?> = 
        demoMode.demoActiveState.flatMapLatest { active ->
            if (active) {
                val d = demoMode.getDemoNetworkHealth()
                val s = demoMode.getDemoSentiment()
                flowOf(
                    NetworkHealth(
                        btcHashrate = "${d.btcGasFeeSat} sat",
                        btcMempool = "${d.mempoolBacklog} txs",
                        ethGas = "${d.ethGasFeeGwei} gwei",
                        fearGreedIndex = s.fearGreedIndex,
                        fearGreedLabel = s.fearGreedLabel,
                        socialPulse = s.redditPositive,
                        socialPulseLabel = if (s.redditPositive > 60) "Bullish" else "Neutral",
                    )
                )
            } else {
                _networkHealth.debounce(2000L)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
    val uiState: StateFlow<DashboardUiState> = demoMode.demoActiveState.flatMapLatest { active ->
        if (active) {
            flowOf(DashboardUiState.Success(
                prices = demoMode.getDemoPriceTickers().map { it.toDomain() },
                isAdmin = isAdmin.value,
                whaleSignal = WhaleSignal.NEUTRAL,
                dailyPick = null,
                shortPulse = "Demo market pulse is active.",
                cloudWhaleAlerts = emptyList()
            ))
        } else {
            combine(
                observeTickerUseCase().debounce(500L),
                tierAccessManager.currentTier,
                _whaleSignal.debounce(1000L),
                _dailyPick,
                _shortPulse,
                cloudState,
                _liquidationSummary
            ) { params ->
                @Suppress("UNCHECKED_CAST")
                val prices = params[0] as List<CoinPrice>
                val tier = params[1] as AccessTier
                val whaleSignal = params[2] as WhaleSignal
                val dailyPick = params[3] as? DailyAIPick
                val shortPulse = params[4] as String
                val cloud = params[5] as? com.cryptodept.data.remote.model.CloudTerminalState
                val liqSummary = params[6] as? LiquidationSummary

                if (prices.isEmpty()) {
                    DashboardUiState.Error("NO_MARKET_DATA: CHECK_CONNECTION")
                } else {
                    // ULTRA-OPTIMIZED TRAFFIC (M1.3): 
                    // 1. Ticker shows Top 3 (Global) + User Watchlist (Limited)
                    val limit = if (tier.canAccess(AccessTier.PRO)) 15 else 3
                    val filteredPrices = (prices.take(3) + prices.filter { it.isTracked }.take(limit)).distinctBy { it.id }

                    DashboardUiState.Success(
                        prices = filteredPrices, 
                        isAdmin = tier == AccessTier.ADMIN,
                        whaleSignal = whaleSignal,
                        dailyPick = dailyPick,
                        shortPulse = shortPulse,
                        cloudWhaleAlerts = cloud?.whaleAlerts ?: emptyList(),
                        pricesLastUpdated = prices.firstOrNull()?.lastUpdated ?: 0L,
                        narrativeLastUpdated = cloud?.lastUpdateTimestamp ?: 0L,
                        fearGreedLastUpdated = cloud?.lastUpdateTimestamp ?: 0L,
                        whaleDataLastUpdated = cloud?.lastUpdateTimestamp ?: 0L,
                        liquidationSummary = liqSummary
                    )
                }
            }.onStart {
                analytics.logScreenView("DASHBOARD")
                fetchNetworkHealth()
                loadTierAwareData()
            }
        }
    }.catch { e ->
        emit(DashboardUiState.Error(e.message ?: "UNKNOWN ERROR"))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState.Loading)

    private val _whaleSignal = MutableStateFlow(WhaleSignal.NEUTRAL)
    private val _dailyPick = MutableStateFlow<DailyAIPick?>(null)
    private val _shortPulse = MutableStateFlow("")

    private val _liquidationSummary = MutableStateFlow<LiquidationSummary?>(null)
    
    private val _currentHeroCoin = MutableStateFlow<CoinPrice?>(null)

    private val _btcChartData = MutableStateFlow<List<OHLCData>>(emptyList())
    val btcChartData: StateFlow<List<OHLCData>> = _btcChartData.asStateFlow()

    private fun loadBtcChartData() {
        viewModelScope.launch(Dispatchers.IO) {
            getOHLCUseCase("bitcoin", 1)
                .collect { _btcChartData.value = it }
        }
        viewModelScope.launch(Dispatchers.IO) {
            refreshOHLCUseCase("bitcoin", 1)
        }
    }

    private fun loadTierAwareData() {
        viewModelScope.launch {
            // Macro Intelligence for all tiers
            fetchMacroIntelligence()
            
            // Whale Insight for Free/Pro
            _whaleSignal.value = getWhaleInsightUseCase.execute()
            
            // Daily AI Pick - Task: Always available as free service on dashboard
            _dailyPick.value = getDailyAIPickUseCase.execute()

            // BTC Chart for Dashboard
            loadBtcChartData()
        }
    }

    private fun fetchMacroIntelligence() {
        viewModelScope.launch(Dispatchers.IO) {
            getMacroIntelligenceUseCase().onSuccess {
                _macroIntelligence.value = it
            }.onFailure {
                Log.e("Dashboard", "Failed to fetch macro intelligence: ${it.message}")
            }
        }
    }

    private fun com.cryptodept.util.DemoTicker.toDomain() = CoinPrice(
        id = symbol.lowercase(),
        symbol = symbol,
        name = symbol,
        currentPrice = price,
        priceChange24h = (change24h / 100) * price,
        priceChangePercentage24h = change24h,
        marketCap = 1_000_000_000.0,
        totalVolume = 100_000_000.0,
        high24h = price * 1.05,
        low24h = price * 0.95,
        lastUpdated = System.currentTimeMillis(),
        isTracked = true
    )

    val isAdmin: StateFlow<Boolean> = subscription.isAdmin.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    private val _isCloudLive = MutableStateFlow(false)
    val isCloudLive: StateFlow<Boolean> = combine(cloudState, demoMode.demoActiveState) { cloud, active ->
        if (active) true
        else cloud != null && (System.currentTimeMillis() - cloud.lastUpdateTimestamp) < 300_000 // 5 mins
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _broadcastMessage = MutableStateFlow("")
    val broadcastMessage: StateFlow<String> = _broadcastMessage.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val integrityLogs = integrityService.logs

    private var isScanLineTriggered = false
    private fun triggerScanLine() {
        if (isScanLineTriggered) return
        isScanLineTriggered = true
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(150L)
            _isRefreshing.value = false
            delay(1000L) // Debounce scan line
            isScanLineTriggered = false
        }
    }

    init {
        updateBroadcastMessage()
        startLiquidationRefresh()
    }

    private fun startLiquidationRefresh() {
        viewModelScope.launch {
            while (isActive) {
                val hero = _currentHeroCoin.value
                if (hero != null) {
                    loadLiquidationData(hero.symbol, hero.currentPrice)
                }
                delay(60_000)
            }
        }
    }

    fun loadLiquidationData(symbol: String, currentPrice: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            getLiquidationSummaryUseCase(symbol, currentPrice)
                .onSuccess { _liquidationSummary.value = it }
                .onFailure { /* Catch silently */ }
        }
    }

    fun onHeroCoinChanged(coin: CoinPrice) {
        if (_currentHeroCoin.value?.id == coin.id) return
        _currentHeroCoin.value = coin
        loadLiquidationData(coin.symbol, coin.currentPrice)
    }

    private fun updateBroadcastMessage() {
        _broadcastMessage.value = remoteConfig.getTerminalBroadcastMsg()
    }

    fun setAdminStatus(enabled: Boolean) {
        viewModelScope.launch {
            subscription.setAdminStatus(enabled)
        }
    }

    private fun fetchNetworkHealth() {
        viewModelScope.launch(Dispatchers.IO) {
            getNetworkHealthUseCase().onSuccess { health ->
                _networkHealth.value = health
                logService.addEvent(EventType.NETWORK_HEALTH, "NETWORK STATUS UPDATED. FG INDEX: ${health.fearGreedIndex}")
                fetchAiSummary(health)
            }.onFailure {
                fetchAiSummary(NetworkHealth(
                    btcHashrate = "N/A",
                    btcMempool = "N/A",
                    ethGas = "N/A",
                    fearGreedIndex = 50,
                    fearGreedLabel = "Neutral",
                    socialPulse = 50,
                    socialPulseLabel = "Neutral"
                ))
            }
        }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            refreshPricesUseCase()
            fetchNetworkHealth()
            fetchMacroIntelligence()
            updateBroadcastMessage()
        }
    }

    private fun fetchAiSummary(health: NetworkHealth, agentId: String = selectedAgentId.value) {
        viewModelScope.launch(Dispatchers.IO) {
            if (demoMode.isActive()) return@launch

            val macro = _macroIntelligence.value
            val prices = (uiState.value as? DashboardUiState.Success)?.prices ?: emptyList()
            val btc = prices.find { it.symbol.lowercase() == "btc" }
            
            val snapshot = MarketDataSnapshot(
                price = btc?.currentPrice ?: 0.0,
                rsi = cloudState.value?.marketData?.get("bitcoin")?.rsi ?: 50.0,
                macdSignal = cloudState.value?.marketData?.get("bitcoin")?.macdSignal ?: "N/A",
                ema50Signal = cloudState.value?.marketData?.get("bitcoin")?.trend ?: "N/A",
                ema200Signal = "N/A",
                bollingerPosition = "N/A",
                fundingRate = 0.0,
                fundingLevel = "N/A",
                longLiquidations24h = macro?.totalLiquidations24h?.longsUsd ?: 0.0,
                shortLiquidations24h = macro?.totalLiquidations24h?.shortsUsd ?: 0.0,
                fearGreedIndex = health.fearGreedIndex,
                newsSentiment = "NEUTRAL",
                wyckoffPhase = "N/A",
                elliottWave = "N/A",
                riskScore = cloudState.value?.macroBriefing?.riskScore ?: riskEngine.currentScore.value,
                priceChange24h = btc?.priceChangePercentage24h ?: 0.0,
                btcDominance = macro?.btcDominance ?: 50.0,
                sp500Change = 0.0,
                dxyChange = 0.0,
            )

            var currentReport = ""
            try {
                withTimeout(30000) { 
                    aiGenerator.generateShortSummaryStream(agentId, snapshot)
                        .catch { e -> Log.e("Dashboard", "AI Stream failed: ${e.message}") }
                        .collect { chunk ->
                            currentReport += chunk
                            _localAgentReports.value = _localAgentReports.value.toMutableMap().apply {
                                put(agentId, currentReport)
                            }
                        }
                }
            } catch (e: Exception) {
                Log.w("Dashboard", "AI Stream timed out or error")
            }
        }
    }
}

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(
        val prices: List<CoinPrice>,
        val isAdmin: Boolean,
        val whaleSignal: WhaleSignal = WhaleSignal.NEUTRAL,
        val dailyPick: DailyAIPick? = null,
        val shortPulse: String = "",
        val cloudWhaleAlerts: List<com.cryptodept.data.remote.model.CloudWhaleAlert> = emptyList(),
        val pricesLastUpdated: Long = 0L,
        val narrativeLastUpdated: Long = 0L,
        val fearGreedLastUpdated: Long = 0L,
        val whaleDataLastUpdated: Long = 0L,
        val liquidationSummary: LiquidationSummary? = null,
    ) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}
