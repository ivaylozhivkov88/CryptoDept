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

import com.cryptodept.util.MarketSession
import kotlin.math.abs

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
    val tierAccessManager: TierAccessManager,
    private val firebaseDataSource: com.cryptodept.data.remote.source.FirebaseRemoteDataSource,
    private val getWhaleInsightUseCase: GetWhaleInsightUseCase,
    private val getDailyAIPickUseCase: GetDailyAIPickUseCase,
    private val getMacroIntelligenceUseCase: GetMacroIntelligenceUseCase,
    private val getOHLCUseCase: GetOHLCUseCase,
    private val refreshOHLCUseCase: RefreshOHLCUseCase,
    private val getLiquidationSummaryUseCase: GetLiquidationSummaryUseCase,
    private val integrityService: com.cryptodept.domain.manager.SystemIntegrityService,
    private val briefingRepository: com.cryptodept.domain.repository.BriefingRepository,
    private val sessionManager: com.cryptodept.util.MarketSessionManager,
    private val macroRepository: com.cryptodept.domain.repository.MacroRepository,
) : ViewModel() {

    // 1. ПЪРВО ДЕФИНИРАМЕ ВСИЧКИ БАЗОВИ СЪСТОЯНИЯ (Private Flows)
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _macroIntelligence = MutableStateFlow<MacroIntelligence?>(null)
    private val _networkHealth = MutableStateFlow<NetworkHealth?>(null)
    private val _aiSummary = MutableStateFlow("ANALYZING MARKET DYNAMICS...")
    private val _agentStatuses = MutableStateFlow<Map<String, AgentStatus>>(emptyMap())
    private val _whaleSignal = MutableStateFlow(WhaleSignal.NEUTRAL)
    private val _dailyPick = MutableStateFlow<DailyAIPick?>(null)
    private val _shortPulse = MutableStateFlow("")
    private val _liquidationSummary = MutableStateFlow<LiquidationSummary?>(null)
    private val _currentHeroCoin = MutableStateFlow<CoinPrice?>(null)
    private val _btcChartData = MutableStateFlow<List<OHLCData>>(emptyList())
    private val _localAgentReports = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _selectedAgentId = MutableStateFlow("AGENT-SENTINEL")
    private val _broadcastMessage = MutableStateFlow("")
    private val _oneTimeEvent = MutableSharedFlow<DashboardOneTimeEvent>()

    // 2. СЛЕД ТОВА ДЕФИНИРАМЕ ПУБЛИЧНИТЕ FLOWS, КОИТО ЗАВИСЯТ ОТ ТЯХ
    val cloudState: StateFlow<com.cryptodept.data.remote.model.CloudTerminalState?> = 
        firebaseDataSource.getTerminalState()
            .onEach { triggerScanLine() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val macroIntelligence: StateFlow<MacroIntelligence?> = combine(
        macroRepository.observeMacroIntelligence(),
        cloudState
    ) { local, cloud ->
        val cloudMacro = cloud?.macroBriefing?.let { b ->
            MacroIntelligence(
                btcDominance = b.btcDominance,
                btcDominanceDelta24h = 0.0,
                ethGasGwei = b.ethGasGwei,
                globalMarketCapUsd = b.globalMarketCapUsd,
                altcoinSeasonIndex = b.altcoinSeasonIndex,
                globalLiquidityUsd = b.globalLiquidityUsd,
                gasPrediction = b.gasPrediction,
                totalLiquidations1h = LiquidationSnapshot(b.liquidations1h.totalUsd, b.liquidations1h.longsUsd, b.liquidations1h.shortsUsd, cloud.lastUpdateTimestamp),
                totalLiquidations24h = LiquidationSnapshot(b.liquidations24h.totalUsd, b.liquidations24h.longsUsd, b.liquidations24h.shortsUsd, cloud.lastUpdateTimestamp)
            )
        }
        
        // Internet-First (Local Repo Data contains direct API fetches)
        // If local (internet-fetched) is valid and looks real, use it.
        if (local != null && local.altcoinSeasonIndex != 50 && local.globalMarketCapUsd > 0) {
            return@combine local
        }
        
        // Otherwise use cloud as second source of truth
        return@combine cloudMacro ?: local
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val networkHealth: StateFlow<NetworkHealth?> = combine(
        _networkHealth, 
        cloudState, 
        demoMode.demoActiveState
    ) { local, cloud, active ->
        if (active) {
            val d = demoMode.getDemoNetworkHealth()
            val s = demoMode.getDemoSentiment()
            return@combine NetworkHealth(d.btcGasFeeSat.toString(), d.mempoolBacklog.toString(), d.ethGasFeeGwei.toString(), s.fearGreedIndex, s.fearGreedLabel)
        }
        
        val cloudHealth = cloud?.macroBriefing?.let { b ->
            NetworkHealth("N/A", "N/A", b.ethGasGwei.toString(), b.fearGreedIndex, "Cloud")
        }
        
        // Prefer local (live) if available, otherwise cloud
        val result = if (local != null && local.fearGreedIndex > 0) local else cloudHealth
        
        // Align Fear & Greed with CMC style (CMC is usually 4-5 points higher than Alternative.me)
        result?.let {
            if (it.fearGreedIndex in 1..40) {
                it.copy(fearGreedIndex = it.fearGreedIndex + 4)
            } else it
        } ?: result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentTier: StateFlow<AccessTier> = tierAccessManager.currentTier
    val isAdmin: StateFlow<Boolean> = subscription.isAdmin.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val broadcastMessage: StateFlow<String> = _broadcastMessage.asStateFlow()
    val oneTimeEvent = _oneTimeEvent.asSharedFlow()
    val integrityLogs = integrityService.logs
    val btcChartData: StateFlow<List<OHLCData>> = _btcChartData.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
    val uiState: StateFlow<DashboardUiState> = demoMode.demoActiveState.flatMapLatest { active ->
        if (active) {
            flowOf(DashboardUiState.Success(
                prices = demoMode.getDemoPriceTickers().map { it.toDomain() },
                isAdmin = isAdmin.value,
                whaleSignal = WhaleSignal.NEUTRAL,
                dailyPick = null,
                shortPulse = "Demo Mode Active",
                cloudWhaleAlerts = emptyList()
            ))
        } else {
            val sessionFlow = flow {
                while(true) {
                    emit(sessionManager.getCurrentSession())
                    kotlinx.coroutines.delay(60_000) // Update every minute
                }
            }

            combine(
                observeTickerUseCase().debounce(500L),
                tierAccessManager.currentTier,
                _whaleSignal,
                _dailyPick,
                cloudState,
                _liquidationSummary,
                sessionFlow,
                briefingRepository.getAllBriefings().map { it.firstOrNull()?.summary }
            ) { params ->
                @Suppress("UNCHECKED_CAST")
                val prices = params[0] as List<CoinPrice>
                val tier = params[1] as AccessTier
                val whale = params[2] as WhaleSignal
                val pick = params[3] as? DailyAIPick
                @Suppress("UNCHECKED_CAST")
                val cloud = params[4] as? com.cryptodept.data.remote.model.CloudTerminalState
                val liq = params[5] as? LiquidationSummary
                val session = params[6] as com.cryptodept.util.MarketSession
                val brief = params[7] as? String
                
                if (prices.isEmpty()) DashboardUiState.Error("NO_DATA")
                else DashboardUiState.Success(
                    prices = prices.take(10),
                    isAdmin = tier == AccessTier.ADMIN,
                    whaleSignal = whale,
                    dailyPick = pick,
                    shortPulse = "Live",
                    cloudWhaleAlerts = cloud?.whaleAlerts ?: emptyList(),
                    liquidationSummary = liq,
                    pricesLastUpdated = cloud?.lastUpdateTimestamp ?: 0L,
                    whaleDataLastUpdated = cloud?.lastUpdateTimestamp ?: 0L,
                    currentSession = session,
                    sessionBrief = brief
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState.Loading)

    val isCloudLive: StateFlow<Boolean> = combine(cloudState, demoMode.demoActiveState) { cloud, active ->
        active || (cloud != null && (System.currentTimeMillis() - cloud.lastUpdateTimestamp) < 600_000)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true) // Default to true to avoid immediate error UI

    val aiSummary: StateFlow<String> = combine(
        _aiSummary, 
        cloudState, 
        _selectedAgentId,
        briefingRepository.getAllBriefings().map { it.firstOrNull()?.summary }.onStart { emit(null) }
    ) { local, cloud, id, latestBriefing ->
        // Priority: Cloud Narrative > Cloud Agent Report > Local Database Briefing > Local Initializing State
        val cloudReport = cloud?.agentReports?.get(id)
        
        when {
            !cloud?.aiNarrative.isNullOrBlank() -> cloud?.aiNarrative!!
            cloudReport != null && !cloudReport.contains("SIGNAL_LOST") -> cloudReport
            !latestBriefing.isNullOrBlank() -> latestBriefing
            else -> local
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "ANALYZING...")

    val agentStatuses: StateFlow<Map<String, AgentStatus>> = combine(_agentStatuses, cloudState) { local, cloud ->
        cloud?.agentStatuses?.mapValues { try { AgentStatus.valueOf(it.value) } catch(e: Exception) { AgentStatus.READY } } ?: local
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    // 3. ЛОГИКА И ИНИЦИАЛИЗАЦИЯ
    private var isScanLineTriggered = false
    private fun triggerScanLine() {
        if (isScanLineTriggered) return
        isScanLineTriggered = true
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                delay(150L)
                _isRefreshing.value = false
            } catch (e: Exception) {
                // Предотвратяваме OOM чрез логване вместо рестартиране
                Log.e("DashboardVM", "Scanline error", e)
            } finally {
                delay(1000L)
                isScanLineTriggered = false
            }
        }
    }

    init {
        refresh()
        startLiquidationRefresh()
    }

    private fun startLiquidationRefresh() {
        viewModelScope.launch {
            while (isActive) {
                _currentHeroCoin.value?.let { loadLiquidationData(it.symbol, it.currentPrice) }
                delay(60_000)
            }
        }
    }

    fun loadLiquidationData(symbol: String, price: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            getLiquidationSummaryUseCase(symbol, price).onSuccess { _liquidationSummary.value = it }
        }
    }

    fun onHeroCoinChanged(coin: CoinPrice) {
        _currentHeroCoin.value = coin
        loadLiquidationData(coin.symbol, coin.currentPrice)
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            refreshPricesUseCase()
            getNetworkHealthUseCase().onSuccess { _networkHealth.value = it }
            getMacroIntelligenceUseCase().onSuccess { _macroIntelligence.value = it }
            _whaleSignal.value = getWhaleInsightUseCase.execute()
            _dailyPick.value = getDailyAIPickUseCase.execute()
        }
    }

    private fun com.cryptodept.util.DemoTicker.toDomain() = CoinPrice(
        id = symbol.lowercase(), symbol = symbol, name = symbol,
        currentPrice = price, priceChange24h = 0.0, priceChangePercentage24h = change24h,
        marketCap = 0.0, totalVolume = 0.0, high24h = 0.0, low24h = 0.0,
        lastUpdated = System.currentTimeMillis()
    )
}

sealed class DashboardOneTimeEvent {
    object ShowPromoSnackbar : DashboardOneTimeEvent()
}
