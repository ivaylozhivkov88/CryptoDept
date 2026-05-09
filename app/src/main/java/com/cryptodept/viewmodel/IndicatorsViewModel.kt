package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.repository.CryptoRepository
import com.cryptodept.domain.usecase.TechnicalAnalysisEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IndicatorScanResult(
    val coinId: String,
    val symbol: String,
    val rsi: Double,
    val macdValue: Double,
    val signal: String,
    val rsiStatus: String, // OVERSOLD, OVERBOUGHT, NEUTRAL
)

sealed class IndicatorsUiState {
    object Loading : IndicatorsUiState()

    data class Success(
        val results: List<IndicatorScanResult>,
    ) : IndicatorsUiState()

    data class Error(
        val message: String,
    ) : IndicatorsUiState()
}

@HiltViewModel
class IndicatorsViewModel
    @Inject
    constructor(
        private val repository: CryptoRepository,
        private val taEngine: TechnicalAnalysisEngine,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<IndicatorsUiState>(IndicatorsUiState.Loading)
        val uiState = _uiState.asStateFlow()

        private val TRACKED_COINS =
            listOf(
                "bitcoin",
                "ethereum",
                "ripple",
                "solana",
                "cardano",
                "polkadot",
                "dogecoin",
                "chainlink",
                "shiba-inu",
                "litecoin",
                "avalanche-2",
                "tron",
                "matic-network",
                "stellar",
                "cosmos",
            )

        init {
            scanIndicators()
        }

        fun scanIndicators() {
            viewModelScope.launch {
                _uiState.value = IndicatorsUiState.Loading
                try {
                    val scanJobs =
                        TRACKED_COINS.map { coinId ->
                            async {
                                val ohlc = repository.getOHLCData(coinId, 30)
                                if (ohlc.isNotEmpty()) {
                                    val prices = ohlc.map { it.close }
                                    val rsi = taEngine.calculateRSI(prices)
                                    val macd = taEngine.calculateMACD(prices)

                                    val rsiStatus =
                                        when {
                                            rsi <= 30 -> "OVERSOLD"
                                            rsi >= 70 -> "OVERBOUGHT"
                                            else -> "NEUTRAL"
                                        }

                                    val macdHist = macd.histogram.lastOrNull() ?: 0.0
                                    val signal = if (macdHist > 0) "BULLISH_CROSS" else "BEARISH_CROSS"

                                    IndicatorScanResult(
                                        coinId = coinId,
                                        symbol = coinId.take(3).uppercase(),
                                        rsi = rsi,
                                        macdValue = macdHist,
                                        signal = signal,
                                        rsiStatus = rsiStatus,
                                    )
                                } else {
                                    null
                                }
                            }
                        }

                    val results = scanJobs.awaitAll().filterNotNull()
                    _uiState.value = IndicatorsUiState.Success(results)
                } catch (e: Exception) {
                    _uiState.value = IndicatorsUiState.Error(e.message ?: "SCAN_FAILED")
                }
            }
        }
    }
