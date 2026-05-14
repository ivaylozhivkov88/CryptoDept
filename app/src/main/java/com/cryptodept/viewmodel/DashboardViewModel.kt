package com.cryptodept.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.model.*
import com.cryptodept.domain.usecase.*
import com.cryptodept.domain.manager.DashboardLogService
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
    private val getActionRecommendationUseCase: GetActionRecommendationUseCase,
    private val aiGenerator: AIReportGenerator,
    private val riskEngine: RiskScoreEngine,
    private val logService: DashboardLogService,
    private val analytics: AnalyticsService,
    private val preferencesService: com.cryptodept.data.datastore.PreferencesService,
    private val agentCoordinator: MultiAgentCoordinator
) : ViewModel() {

    private val _tutorialStep = MutableStateFlow<TutorialStep?>(null)
    val tutorialStep: StateFlow<TutorialStep?> = _tutorialStep.asStateFlow()

    val focusModeEnabled: StateFlow<Boolean> = preferencesService.focusModeEnabled.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    fun startTutorial() {
        _tutorialStep.value = TutorialStep.WELCOME
    }

    fun nextStep() {
        val current = _tutorialStep.value ?: return
        val nextIndex = current.ordinal + 1
        if (nextIndex < TutorialStep.entries.size) {
            _tutorialStep.value = TutorialStep.entries[nextIndex]
        } else {
            _tutorialStep.value = null
        }
    }

    fun skipTutorial() {
        _tutorialStep.value = null
    }

    fun setFocusMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesService.setFocusModeEnabled(enabled)
        }
    }

    val events = logService.events.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _aiSummary = MutableStateFlow("ANALYZING MARKET DYNAMICS...")
    val aiSummary: StateFlow<String> = _aiSummary.asStateFlow()

    private val _isAiStreaming = MutableStateFlow(false)
    val isAiStreaming: StateFlow<Boolean> = _isAiStreaming.asStateFlow()

    private val _agentStatuses = MutableStateFlow<Map<String, AgentStatus>>(emptyMap())
    val agentStatuses: StateFlow<Map<String, AgentStatus>> = _agentStatuses.asStateFlow()

    private val _networkHealth = MutableStateFlow<NetworkHealth?>(null)
    val networkHealth: StateFlow<NetworkHealth?> = _networkHealth.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val uiState: StateFlow<DashboardUiState> = combine(
        observeTickerUseCase(),
        preferencesService.isAdmin
    ) { prices, isAdmin ->
        if (prices.isEmpty()) {
            DashboardUiState.Error("NO_MARKET_DATA: CHECK_CONNECTION")
        } else {
            DashboardUiState.Success(prices, isAdmin)
        }
    }.onStart {
        analytics.logScreenView("DASHBOARD")
        fetchNetworkHealth()
    }.catch { e ->
        emit(DashboardUiState.Error(e.message ?: "UNKNOWN ERROR"))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState.Loading)

    val isAdmin: StateFlow<Boolean> = preferencesService.isAdmin.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    fun setAdminStatus(enabled: Boolean) {
        viewModelScope.launch {
            preferencesService.setAdminStatus(enabled)
        }
    }

    fun activateGodMode() {
        viewModelScope.launch {
            preferencesService.setAdminStatus(true)
            preferencesService.setPowerUserMode(true)
            // If you have a setProStatus in preferencesService, call it here too.
        }
    }

    private fun fetchNetworkHealth() {
        viewModelScope.launch(Dispatchers.IO) {
            getNetworkHealthUseCase().onSuccess { health ->
                _networkHealth.value = health
                logService.addEvent(EventType.NETWORK_HEALTH, "NETWORK STATUS UPDATED. FG INDEX: ${health.fearGreedIndex}")
                fetchAiSummary(health)
            }.onFailure {
                // Fallback to neutral data if API fails, so AI narrative can still work
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
            _isRefreshing.value = true
            refreshPricesUseCase()
            fetchNetworkHealth()
            _isRefreshing.value = false
        }
    }

    fun computeActionRecommendation(onResult: (String, String) -> Unit) {
        viewModelScope.launch {
            val prices = (uiState.value as? DashboardUiState.Success)?.prices ?: emptyList()
            val rec = getActionRecommendationUseCase(prices)
            onResult(rec.action, rec.explanation)
        }
    }

    private fun fetchAiSummary(health: NetworkHealth) {
        viewModelScope.launch(Dispatchers.IO) {
            _agentStatuses.value = mapOf(
                "SENTINEL" to AgentStatus.SCANNING,
                "SCOUT" to AgentStatus.READY,
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
                longLiquidations24h = 0.0,
                shortLiquidations24h = 0.0,
                fearGreedIndex = health.fearGreedIndex,
                newsSentiment = "NEUTRAL",
                wyckoffPhase = "N/A",
                elliottWave = "N/A",
                riskScore = riskEngine.currentScore.value,
                priceChange24h = btc?.priceChangePercentage24h ?: 0.0,
                btcDominance = 50.0,
                sp500Change = 0.0,
                dxyChange = 0.0,
            )

            // Simulate agentic scan for UI reveal
            delay(500)
            _agentStatuses.value = _agentStatuses.value.toMutableMap().apply { 
                put("SENTINEL", AgentStatus.SUCCESS)
                put("SCOUT", AgentStatus.SCANNING)
                put("SYSTRACE", AgentStatus.SCANNING)
            }
            delay(400)
            _agentStatuses.value = _agentStatuses.value.toMutableMap().apply { 
                put("SCOUT", AgentStatus.SUCCESS)
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
            
            // Step 1: Set local report as immediate baseline
            _aiSummary.value = report.summary
            _isAiStreaming.value = true
            
            var aiStarted = false
            try {
                withTimeout(30000) { // Give AI more time, but don't hang forever
                    aiGenerator.generateShortSummaryStream(snapshot)
                        .catch { e -> 
                            Log.e("Dashboard", "AI Stream failed: ${e.message}")
                        }
                        .collect { chunk ->
                            if (!aiStarted) {
                                // Step 2: First AI chunk received! Clear local summary to show AI narrative
                                _aiSummary.value = ""
                                aiStarted = true
                            }
                            _aiSummary.value += chunk
                        }
                }
            } catch (e: Exception) {
                Log.w("Dashboard", "AI Stream timed out or error")
            } finally {
                _isAiStreaming.value = false
                // If AI never started or failed, the local summary is already there
            }
        }
    }
}

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(val prices: List<CoinPrice>, val isAdmin: Boolean) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}
