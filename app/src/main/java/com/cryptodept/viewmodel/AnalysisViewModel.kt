package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.data.datastore.SubscriptionAccessManager
import com.cryptodept.data.remoteconfig.RemoteConfigService
import com.cryptodept.domain.model.*
import com.cryptodept.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.Locale
import javax.inject.Inject

sealed class AnalysisUiState {
    object Loading : AnalysisUiState()
    data class Success(val result: DeepAnalysisResult) : AnalysisUiState()
    data class Error(val message: String) : AnalysisUiState()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val runDeepAnalysis: RunDeepAnalysisUseCase,
    private val generateReport: GenerateAnalysisReportUseCase,
    private val observeAnalysisHistory: ObserveAnalysisHistoryUseCase,
    private val subscription: SubscriptionAccessManager,
    private val remoteConfig: RemoteConfigService,
    private val demoMode: com.cryptodept.util.DemoModeProvider,
) : ViewModel() {

    val isAdmin = subscription.isAdmin.stateIn(
        viewModelScope, 
        SharingStarted.WhileSubscribed(5000), 
        false,
    )
    
    val isPro = subscription.isPro.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    private val _selectedCoin = MutableStateFlow("bitcoin")
    private val _selectedDays = MutableStateFlow(30)

    init {
        observeDemoMode()
    }

    private fun observeDemoMode() {
        viewModelScope.launch {
            demoMode.demoActiveState.collectLatest { active ->
                if (active) {
                    // Trigger a re-analysis when demo starts to load demo data
                    val current = _selectedCoin.value
                    _selectedCoin.value = ""
                    delay(10)
                    _selectedCoin.value = current
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val analysisState: StateFlow<AnalysisUiState> = demoMode.demoActiveState.flatMapLatest { active ->
        if (active) {
            val d = demoMode.getDemoAnalysis()
            flowOf(
                AnalysisUiState.Success(
                    DeepAnalysisResult(
                        coinId = d.coinSymbol,
                        compositeSignal = CompositeSignal(
                            strength = SignalStrength.BUY,
                            bullishCount = 4,
                            bearishCount = 1,
                            neutralCount = 1,
                            indicators = listOf(
                                IndicatorStatus("RSI", String.format(Locale.US, "%.1f", d.rsi), Sentiment.NEUTRAL),
                                IndicatorStatus("MACD", d.macdLabel, Sentiment.BULLISH),
                                IndicatorStatus("EMA50", "SUPPORT", Sentiment.BULLISH),
                            ),
                            confidence = d.confidence,
                        ),
                        currentPrice = d.currentPrice,
                        ohlcData = demoMode.getDemoOhlc(),
                        patterns = demoMode.getDemoPatterns(),
                        fibonacci = mapOf("0.618" to (d.currentPrice * 0.98), "0.5" to (d.currentPrice * 0.95)),
                        rsiValue = d.rsi,
                        sentiment = SentimentResult(
                            symbol = d.coinSymbol,
                            verdict = SentimentVerdict.BULLISH,
                            bullishPercent = 65,
                            bearishPercent = 15,
                            neutralPercent = 20,
                            totalAnalyzed = 142,
                        ),
                        traces = demoMode.getDemoTraces(),
                    )
                )
            )
        } else {
            combine(
                _selectedCoin, 
                _selectedDays,
            ) { coin, days -> coin to days }
                .flatMapLatest { (coin, days) ->
                    flow<AnalysisUiState> {
                        emit(AnalysisUiState.Loading)
                        try {
                            withTimeout(20000) {
                                runDeepAnalysis.execute(coin, days)
                                    .onSuccess { emit(AnalysisUiState.Success(it)) }
                                    .onFailure { emit(AnalysisUiState.Error(it.message ?: "UNKNOWN ERROR")) }
                            }
                        } catch (_: TimeoutCancellationException) {
                            emit(AnalysisUiState.Error("ANALYSIS_TIMEOUT: NETWORK_CONGESTION_DETECTED"))
                        } catch (e: Exception) {
                            emit(AnalysisUiState.Error(e.message ?: "SYSTEM_ERROR"))
                        }
                    }
                }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AnalysisUiState.Loading)

    fun loadAnalysis(coinId: String) {
        viewModelScope.launch {
            delay(500) // Visual confirmation of reload
            // Handle both "bitcoin" (ID) and "BTC" (Symbol)
            val cleanId = coinId.lowercase()
            _selectedCoin.value = if (cleanId == "btc") "bitcoin" else if (cleanId == "eth") "ethereum" else cleanId
        }
    }

    val trackedCoins: StateFlow<List<String>> = demoMode.demoActiveState.flatMapLatest { active ->
        if (active) {
            flowOf(demoMode.getDemoTrackedCoins())
        } else {
            observeAnalysisHistory().map { list -> 
                list.ifEmpty { listOf("BTC", "ETH", "SOL", "BNB", "XRP", "DOGE", "ADA", "TRX", "DOT", "LINK", "AVAX", "SHIB", "TON", "XLM", "SUI") }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("BTC", "ETH", "SOL"))

    private val _aiReport = MutableStateFlow<String?>(null)
    val aiReport: StateFlow<String?> = _aiReport.asStateFlow()

    private val _isAiStreaming = MutableStateFlow(false)
    val isAiStreaming: StateFlow<Boolean> = _isAiStreaming.asStateFlow()

    fun generateAIReport(result: DeepAnalysisResult) {
        viewModelScope.launch {
            if (!isPro.value) {
                val count = subscription.getAiReportsCountToday()
                val limit = remoteConfig.getFreeAiLimitDaily()
                if (count >= limit) {
                    _aiReport.value = ">>> LIMIT_REACHED: FREE OPERATORS ARE LIMITED TO $limit REPORTS DAILY.\n\nUPGRADE TO PRO TO UNLOCK UNLIMITED AI INTELLIGENCE."
                    return@launch
                }
            }

            _aiReport.value = ""
            _isAiStreaming.value = true
            
            if (!isPro.value) {
                subscription.incrementAiReportsCount()
            }

            generateReport.execute(result)
                .onCompletion { _isAiStreaming.value = false }
                .collect { chunk ->
                    _aiReport.value = (_aiReport.value ?: "") + chunk
                }
        }
    }

    fun dismissAiReport() {
        _aiReport.value = null
    }

    fun setAdminStatus(isAdmin: Boolean) {
        viewModelScope.launch { subscription.setAdminStatus(isAdmin) }
    }
}
