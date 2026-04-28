package com.cryptodept.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.model.*
import com.cryptodept.domain.repository.CryptoRepository
import com.cryptodept.domain.repository.ChartRepository
import com.cryptodept.domain.usecase.TechnicalAnalysisEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

sealed class AnalysisUiState {
    object Loading : AnalysisUiState()
    data class Success(
        val coinId: String,
        val compositeSignal: CompositeSignal,
        val currentPrice: Double,
        val ohlcData: List<OHLCData>,
        val patterns: List<TechnicalAnalysisEngine.PatternDetection>,
        val fibonacci: Map<String, Double>
    ) : AnalysisUiState()
    data class Error(val message: String) : AnalysisUiState()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val cryptoRepository: CryptoRepository,
    private val chartRepository: ChartRepository,
    private val taEngine: TechnicalAnalysisEngine
) : ViewModel() {

    private val _selectedCoin = MutableStateFlow("bitcoin")
    private val _selectedDays = MutableStateFlow(30)

    val analysisState: StateFlow<AnalysisUiState> = combine(
        _selectedCoin,
        _selectedDays
    ) { coin, days -> Pair(coin, days) }
        .flatMapLatest { (coin, days) ->
            flow {
                emit(AnalysisUiState.Loading)
                try {
                    val normalizedId = when (coin.lowercase()) {
                        "btc" -> "bitcoin"
                        "eth" -> "ethereum"
                        "xrp" -> "ripple"
                        "sol" -> "solana"
                        "ada" -> "cardano"
                        "dot" -> "polkadot"
                        "doge" -> "dogecoin"
                        "link" -> "chainlink"
                        "shib" -> "shiba-inu"
                        "ltc" -> "litecoin"
                        else -> coin.lowercase()
                    }

                    // Force refresh OHLC data
                    chartRepository.refreshOHLCData(normalizedId, days)
                    
                    val ohlcData = cryptoRepository.getOHLCData(normalizedId, days)
                    
                    if (ohlcData.isEmpty()) {
                        emit(AnalysisUiState.Error("NO DATA RECEIVED FOR $normalizedId"))
                        return@flow
                    }
                    
                    val prices = ohlcData.map { it.close }
                    val rsi = taEngine.calculateRSI(prices)
                    val macd = taEngine.calculateMACD(prices)
                    val patterns = taEngine.detectPatterns(ohlcData)
                    val fib = taEngine.calculateFibonacciLevels(prices.maxOrNull() ?: 0.0, prices.minOrNull() ?: 0.0)
                    
                    val indicators = mutableListOf<IndicatorStatus>()
                    var bullish = 0; var bearish = 0; var neutral = 0

                    val rsiSent = when {
                        rsi < 30 -> Sentiment.BULLISH.also { bullish++ }
                        rsi > 70 -> Sentiment.BEARISH.also { bearish++ }
                        else -> Sentiment.NEUTRAL.also { neutral++ }
                    }
                    indicators.add(IndicatorStatus("RSI", String.format(Locale.US, "%.1f", rsi), rsiSent))

                    val macdHist = macd.histogram.lastOrNull() ?: 0.0
                    val macdSent = if (macdHist > 0) Sentiment.BULLISH.also { bullish++ } 
                                  else Sentiment.BEARISH.also { bearish++ }
                    indicators.add(IndicatorStatus("MACD", if (macdHist > 0) "BULL" else "BEAR", macdSent))

                    val strength = when {
                        bullish >= 2 -> SignalStrength.BUY
                        bearish >= 2 -> SignalStrength.SELL
                        else -> SignalStrength.NEUTRAL
                    }

                    val composite = CompositeSignal(strength, bullish, bearish, neutral, indicators, 0.8f)
                    val currentPrice = cryptoRepository.getCachedPrice(normalizedId)
                    
                    emit(AnalysisUiState.Success(
                        coinId = normalizedId,
                        compositeSignal = composite,
                        currentPrice = currentPrice,
                        ohlcData = ohlcData,
                        patterns = patterns,
                        fibonacci = fib
                    ))
                } catch (e: Exception) {
                    Log.e("CryptoDept", "Analysis error: ${e.message}", e)
                    emit(AnalysisUiState.Error(e.message ?: "UNKNOWN ERROR"))
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalysisUiState.Loading)

    fun selectCoin(coinId: String) { _selectedCoin.value = coinId }
    fun selectDays(days: Int) { _selectedDays.value = days }
    
    // For compatibility with existing screen
    fun loadAnalysis(coinId: String) {
        _selectedCoin.value = coinId
    }
}
