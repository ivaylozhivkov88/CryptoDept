package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.model.*
import com.cryptodept.domain.usecase.*
import com.cryptodept.domain.manager.DashboardLogService
import com.cryptodept.util.AnalyticsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    private val preferencesService: com.cryptodept.data.datastore.PreferencesService
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
        if (nextIndex < TutorialStep.values().size) {
            _tutorialStep.value = TutorialStep.values()[nextIndex]
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

    private val _networkHealth = MutableStateFlow<NetworkHealth?>(null)
    val networkHealth: StateFlow<NetworkHealth?> = _networkHealth.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val uiState: StateFlow<DashboardUiState> = combine(
        observeTickerUseCase(),
        preferencesService.isAdmin
    ) { prices, isAdmin ->
        DashboardUiState.Success(prices, isAdmin) as DashboardUiState
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

            aiGenerator.generateShortSummary(snapshot).onSuccess { summary ->
                _aiSummary.value = summary.uppercase()
            }.onFailure {
                _aiSummary.value = "AI_NARRATIVE_UNAVAILABLE"
            }
        }
    }
}

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(val prices: List<CoinPrice>, val isAdmin: Boolean) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}
