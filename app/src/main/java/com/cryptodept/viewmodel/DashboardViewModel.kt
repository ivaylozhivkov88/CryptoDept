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
import com.cryptodept.domain.usecase.whale.AggregateWhaleActivityUseCase
import com.cryptodept.domain.model.TransactionType
import com.cryptodept.domain.model.WhaleSignal
import com.cryptodept.util.AnalyticsService
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val aggregateWhaleActivityUseCase: AggregateWhaleActivityUseCase,
    private val getDailyAIPickUseCase: GetDailyAIPickUseCase,
    private val getMacroIntelligenceUseCase: GetMacroIntelligenceUseCase,
) : ViewModel() {

    // ============================================================
    // MACRO INTELLIGENCE FLOWS
    // ============================================================
    private val _macroIntelligence = MutableStateFlow<MacroIntelligence?>(null)
    val macroIntelligence: StateFlow<MacroIntelligence?> = _macroIntelligence.asStateFlow()

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

    private val _aiSummary = MutableStateFlow("ANALYZING MARKET DYNAMICS...")
    val aiSummary: StateFlow<String> = _aiSummary.combine(demoMode.demoActiveState) { realSummary, active ->
        if (active) demoMode.getDemoAiNarrative() else realSummary
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "ANALYZING MARKET DYNAMICS...")

    private val _isAiStreaming = MutableStateFlow(false)
    val isAiStreaming: StateFlow<Boolean> = _isAiStreaming.combine(demoMode.demoActiveState) { realStreaming, active ->
        if (active) false else realStreaming
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _agentStatuses = MutableStateFlow<Map<String, AgentStatus>>(emptyMap())
    val agentStatuses: StateFlow<Map<String, AgentStatus>> = _agentStatuses.combine(demoMode.demoActiveState) { realStatuses, active ->
        if (active) demoMode.getDemoAgentStatuses() else realStatuses
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
                _networkHealth
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = demoMode.demoActiveState.flatMapLatest { active ->
        if (active) {
            flowOf(DashboardUiState.Success(
                prices = demoMode.getDemoPriceTickers().map { it.toDomain() },
                isAdmin = isAdmin.value,
                whaleSignal = WhaleSignal.NEUTRAL,
                dailyPick = null,
                shortPulse = "Demo market pulse is active."
            ))
        } else {
            combine(
                observeTickerUseCase(),
                tierAccessManager.currentTier,
                _whaleSignal,
                _dailyPick,
                _shortPulse
            ) { prices, tier, whaleSignal, dailyPick, shortPulse ->
                if (prices.isEmpty()) {
                    DashboardUiState.Error("NO_MARKET_DATA: CHECK_CONNECTION")
                } else {
                    // Task 1.2: Enforce strict 10-coin limit for FREE tier (matching Paywall promise)
                    val filteredPrices = if (tier == AccessTier.FREE) {
                        prices.take(10)
                    } else {
                        prices
                    }

                    DashboardUiState.Success(
                        prices = filteredPrices, 
                        isAdmin = tier == AccessTier.ADMIN,
                        whaleSignal = whaleSignal,
                        dailyPick = dailyPick,
                        shortPulse = shortPulse
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

    private fun loadTierAwareData() {
        viewModelScope.launch {
            // Macro Intelligence for all tiers
            fetchMacroIntelligence()
            
            // Whale Insight for Free/Pro
            _whaleSignal.value = computeWhaleInsight()
            
            // Daily AI Pick - Task: Always available as free service on dashboard
            _dailyPick.value = getDailyAIPickUseCase.execute()
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

    private suspend fun computeWhaleInsight(): WhaleSignal {
        val transactions = try {
            aggregateWhaleActivityUseCase.execute(maxPerChain = 20)
        } catch (e: Exception) {
            return WhaleSignal.NEUTRAL
        }
        
        if (transactions.isEmpty()) return WhaleSignal.NEUTRAL
        
        val now = System.currentTimeMillis()
        val last24h = now - (24 * 60 * 60 * 1000L)
        
        val recentTxs = transactions.filter { it.timestamp >= last24h }
        if (recentTxs.isEmpty()) return WhaleSignal.NEUTRAL

        val totalDeposits = recentTxs
            .filter { it.transactionType == TransactionType.EXCHANGE_DEPOSIT }
            .sumOf { it.amountUsd }
        
        val totalWithdrawals = recentTxs
            .filter { it.transactionType == TransactionType.EXCHANGE_WITHDRAWAL }
            .sumOf { it.amountUsd }
        
        val ratio = if (totalDeposits > 0) totalWithdrawals / totalDeposits else if (totalWithdrawals > 0) 2.0 else 1.0
        
        return when {
            ratio > 1.5 -> WhaleSignal.BULLISH_HEAVY
            ratio > 1.1 -> WhaleSignal.BULLISH
            ratio < 0.66 -> WhaleSignal.BEARISH_HEAVY
            ratio < 0.9 -> WhaleSignal.BEARISH
            else -> WhaleSignal.NEUTRAL
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

    private val _broadcastMessage = MutableStateFlow("")
    val broadcastMessage: StateFlow<String> = _broadcastMessage.asStateFlow()

    init {
        updateBroadcastMessage()
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

    private fun fetchAiSummary(health: NetworkHealth) {
        viewModelScope.launch(Dispatchers.IO) {
            if (demoMode.isActive()) return@launch

            val macro = _macroIntelligence.value

            _agentStatuses.value = mapOf(
                "SENTINEL" to AgentStatus.SCANNING,
                "PULSE" to AgentStatus.READY,
                "SYSTRACE" to AgentStatus.READY,
                "QUANT" to AgentStatus.READY,
                "FISCAL" to AgentStatus.READY
            )
            
            val prices = (uiState.value as? DashboardUiState.Success)?.prices ?: emptyList()
            val btc = prices.find { it.symbol.lowercase() == "btc" }
            
            val snapshot = MarketDataSnapshot(
                price = btc?.currentPrice ?: 0.0,
                rsi = 50.0,
                macdSignal = "N/A",
                ema50Signal = "N/A",
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
                riskScore = riskEngine.currentScore.value,
                priceChange24h = btc?.priceChangePercentage24h ?: 0.0,
                btcDominance = macro?.btcDominance ?: 50.0,
                sp500Change = 0.0, // Could fetch from macro repo if needed
                dxyChange = 0.0,
            )

            delay(500)
            _agentStatuses.value = _agentStatuses.value.toMutableMap().apply { 
                put("SENTINEL", AgentStatus.SUCCESS)
                put("SYSTRACE", AgentStatus.SCANNING)
            }
            delay(400)
            _agentStatuses.value = _agentStatuses.value.toMutableMap().apply { 
                put("SYSTRACE", AgentStatus.SUCCESS)
                put("PULSE", AgentStatus.SCANNING)
                put("FISCAL", AgentStatus.SCANNING)
            }
            delay(500)
            _agentStatuses.value = _agentStatuses.value.toMutableMap().apply { 
                put("PULSE", AgentStatus.SUCCESS)
                put("FISCAL", AgentStatus.SUCCESS)
                put("QUANT", AgentStatus.SCANNING)
            }
            delay(400)
            _agentStatuses.value = _agentStatuses.value.toMutableMap().apply { 
                put("QUANT", AgentStatus.SUCCESS)
            }

            val report = agentCoordinator.runOrchestration(snapshot)
            _aiSummary.value = report.summary
            _isAiStreaming.value = true
            
            var aiStarted = false
            try {
                withTimeout(30000) { 
                    aiGenerator.generateShortSummaryStream(snapshot)
                        .catch { e -> Log.e("Dashboard", "AI Stream failed: ${e.message}") }
                        .collect { chunk ->
                            if (!aiStarted) {
                                _aiSummary.value = ""
                                aiStarted = true
                            }
                            _aiSummary.value += chunk
                            _shortPulse.value = _aiSummary.value
                        }
                }
            } catch (e: Exception) {
                Log.w("Dashboard", "AI Stream timed out or error")
            } finally {
                _isAiStreaming.value = false
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
        val shortPulse: String = ""
    ) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}
