package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.model.*
import com.cryptodept.domain.repository.ChartRepository
import com.cryptodept.domain.repository.CryptoRepository
import com.cryptodept.domain.usecase.MultiTimeframeAnalyzer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MTFViewModel
    @Inject
    constructor(
        private val mtfAnalyzer: MultiTimeframeAnalyzer,
        private val repository: CryptoRepository,
        private val chartRepository: ChartRepository,
    ) : ViewModel() {
        val trackedCoins: StateFlow<List<String>> =
            repository
                .getTrackedCoinPrices()
                .map { prices -> prices.map { it.id } }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        private val _selectedCoin = MutableStateFlow("bitcoin")
        val selectedCoin: StateFlow<String> = _selectedCoin.asStateFlow()

        private val _state = MutableStateFlow<MTFUiState>(MTFUiState.Loading)
        val state: StateFlow<MTFUiState> = _state.asStateFlow()

        init {
            analyze()
        }

        fun selectCoin(coinId: String) {
            _selectedCoin.value = coinId
            analyze()
        }

        fun analyze() {
            viewModelScope.launch {
                _state.value = MTFUiState.Loading
                try {
                    val coinId = _selectedCoin.value
                    val result =
                        withContext(Dispatchers.Default) {
                            if (coinId == "ALL") {
                                val coins = trackedCoins.value
                                if (coins.isEmpty()) throw Exception("NO_TRACKED_COINS")

                                // Ensure data is refreshed for all coins
                                coins.forEach { coin ->
                                    chartRepository.refreshOHLCData(coin, 30)
                                }

                                val allResults = coins.map { mtfAnalyzer.analyze(it) }
                                val avgBullish = allResults.map { it.bullishCount }.average()
                                val avgBearish = allResults.map { it.bearishCount }.average()

                                MTFConsensus(
                                    timeframes =
                                        allResults.first().timeframes.map { tf ->
                                            val tfSignals = allResults.map { it.timeframes.find { it2 -> it2.timeframe == tf.timeframe } }
                                            val tfBullish =
                                                tfSignals.count {
                                                    it?.overallSignal == OverallSignal.STRONG_BUY ||
                                                        it?.overallSignal == OverallSignal.BUY
                                                }
                                            val tfBearish =
                                                tfSignals.count {
                                                    it?.overallSignal == OverallSignal.STRONG_SELL ||
                                                        it?.overallSignal == OverallSignal.SELL
                                                }

                                            tf.copy(
                                                overallSignal =
                                                    when {
                                                        tfBullish > coins.size * 0.7 -> OverallSignal.STRONG_BUY
                                                        tfBullish > coins.size * 0.5 -> OverallSignal.BUY
                                                        tfBearish > coins.size * 0.7 -> OverallSignal.STRONG_SELL
                                                        tfBearish > coins.size * 0.5 -> OverallSignal.SELL
                                                        else -> OverallSignal.NEUTRAL
                                                    },
                                            )
                                        },
                                    bullishCount = avgBullish.toInt(),
                                    bearishCount = avgBearish.toInt(),
                                    neutralCount = allResults.first().timeframes.size - avgBullish.toInt() - avgBearish.toInt(),
                                    consensus =
                                        when {
                                            avgBullish > 3.5 -> OverallSignal.STRONG_BUY
                                            avgBullish > 2.5 -> OverallSignal.BUY
                                            avgBearish > 3.5 -> OverallSignal.STRONG_SELL
                                            avgBearish > 2.5 -> OverallSignal.SELL
                                            else -> OverallSignal.NEUTRAL
                                        },
                                    interpretation = "AGGREGATED CONSENSUS ACROSS ${coins.size} TRACKED ASSETS. SHOWING DOMINANT SIGNALS PER TIMEFRAME.",
                                    tradingBias =
                                        if (avgBullish > avgBearish) {
                                            "BULLISH"
                                        } else if (avgBearish > avgBullish) {
                                            "BEARISH"
                                        } else {
                                            "NEUTRAL"
                                        },
                                )
                            } else {
                                chartRepository.refreshOHLCData(coinId, 30)
                                mtfAnalyzer.analyze(coinId)
                            }
                        }
                    _state.value = MTFUiState.Success(result)
                } catch (e: Exception) {
                    _state.value = MTFUiState.Error(e.message ?: "MTF ANALYSIS FAILED")
                }
            }
        }
    }

sealed class MTFUiState {
    object Loading : MTFUiState()

    data class Success(
        val consensus: MTFConsensus,
    ) : MTFUiState()

    data class Error(
        val message: String,
    ) : MTFUiState()
}
