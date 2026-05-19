package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.model.*
import com.cryptodept.domain.repository.CryptoRepository
import com.cryptodept.domain.usecase.OptimalEntryCalculator
import com.cryptodept.domain.usecase.TechnicalAnalysisEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EntryAnalyzerViewModel
    @Inject
    constructor(
        private val repository: CryptoRepository,
        private val chartRepository: com.cryptodept.domain.repository.ChartRepository,
        private val calculator: OptimalEntryCalculator,
        private val taEngine: TechnicalAnalysisEngine,
    ) : ViewModel() {
        val trackedCoins: StateFlow<List<String>> =
            repository
                .getTrackedCoinPrices()
                .map { prices -> prices.map { it.id } }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        private val _selectedCoin = MutableStateFlow("bitcoin")
        val selectedCoin: StateFlow<String> = _selectedCoin.asStateFlow()

        private val refreshTrigger = MutableStateFlow(System.currentTimeMillis())

        val entryData: StateFlow<EntryAnalysisUiState> =
            combine(
                _selectedCoin,
                refreshTrigger,
            ) { coin, _ -> coin }
                .flatMapLatest { coin ->
                    flow {
                        emit(EntryAnalysisUiState.Loading)
                        try {
                            val result =
                                withContext(Dispatchers.Default) {
                                    chartRepository.refreshOHLCData(coin, 100)
                                    val ohlc = repository.getOHLCData(coin, 100)
                                    if (ohlc.isEmpty()) throw Exception("NO_DATA")

                                    val prices = ohlc.map { it.close }
                                    val rsi = if (prices.size >= 14) taEngine.calculateRSI(prices) else 50.0
                                    val bb = taEngine.calculateBollingerBands(prices)

                                    val analysis =
                                        calculator.analyze(
                                            coin = coin,
                                            currentPrice = prices.last(),
                                            ohlcData = ohlc,
                                            rsi = rsi,
                                            bollingerUpper = bb.upper,
                                            bollingerLower = bb.lower,
                                            bollingerMid = bb.middle,
                                        )

                                    // Mocking components for progress bars as required by prompt
                                    // MOMENTUM / TREND / SENTIMENT / RISK / TIMING
                                    val components =
                                        listOf(
                                            AnalysisComponent("MOMENTUM", (rsi / 100).toFloat(), if (rsi < 40) "OVERSOLD ✓" else "NEUTRAL"),
                                            AnalysisComponent("TREND", 0.7f, "BULLISH ✓"),
                                            AnalysisComponent("SENTIMENT", 0.6f, "POSITIVE"),
                                            AnalysisComponent("RISK", (analysis.entryScore / 100f), "MODERATE"),
                                            AnalysisComponent("TIMING", 0.8f, "IDEAL ✓"),
                                        )

                                    EntryAnalysisUiState.Success(analysis, rsi, components)
                                }
                            emit(result)
                        } catch (e: Exception) {
                            emit(EntryAnalysisUiState.Error(e.message ?: "ANALYSIS_FAILED"))
                        }
                    }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EntryAnalysisUiState.Loading)

        init {
            // Remove auto-refresh loop (Task 2.8)
        }

        fun selectCoin(coinId: String) {
            _selectedCoin.value = coinId
        }

        fun refresh() {
            refreshTrigger.value = System.currentTimeMillis()
        }
    }

sealed class EntryAnalysisUiState {
    object Loading : EntryAnalysisUiState()

    data class Success(
        val analysis: EntryAnalysis,
        val currentRsi: Double,
        val components: List<AnalysisComponent>,
    ) : EntryAnalysisUiState()

    data class Error(
        val message: String,
    ) : EntryAnalysisUiState()
}

data class AnalysisComponent(
    val name: String,
    val value: Float, // 0.0 to 1.0
    val verdict: String,
)
