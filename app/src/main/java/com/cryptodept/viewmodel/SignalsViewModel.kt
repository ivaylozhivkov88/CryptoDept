package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.data.datastore.SubscriptionAccessManager
import com.cryptodept.domain.model.CompositeSignal
import com.cryptodept.domain.model.IndicatorStatus
import com.cryptodept.domain.model.Sentiment
import com.cryptodept.domain.model.SignalStrength
import com.cryptodept.domain.repository.CryptoRepository
import com.cryptodept.domain.usecase.AlphaSignal
import com.cryptodept.domain.usecase.AlphaSignalEngine
import com.cryptodept.domain.usecase.GetOHLCUseCase
import com.cryptodept.domain.usecase.TechnicalAnalysisEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CoinSignal(
    val coinId: String,
    val symbol: String,
    val currentPrice: Double,
    val signal: CompositeSignal,
)

@HiltViewModel
class SignalsViewModel
    @Inject
    constructor(
        private val cryptoRepository: CryptoRepository,
        private val getOHLCUseCase: GetOHLCUseCase,
        private val taEngine: TechnicalAnalysisEngine,
        private val alphaEngine: AlphaSignalEngine,
        private val subscription: SubscriptionAccessManager,
        private val demoMode: com.cryptodept.util.DemoModeProvider,
    ) : ViewModel() {
        private val _signals = MutableStateFlow<List<CoinSignal>>(emptyList())
        val signals: StateFlow<List<CoinSignal>> = combine(_signals, demoMode.demoActiveState) { real, active ->
            if (active) {
                listOf(
                    CoinSignal("bitcoin", "BTC", 103245.5, CompositeSignal(SignalStrength.BUY, 4, 1, 1, emptyList(), 0.8f)),
                    CoinSignal("ethereum", "ETH", 3425.8, CompositeSignal(SignalStrength.BUY, 3, 2, 1, emptyList(), 0.7f)),
                    CoinSignal("solana", "SOL", 212.45, CompositeSignal(SignalStrength.NEUTRAL, 2, 2, 2, emptyList(), 0.5f))
                )
            } else real
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val alphaSignals: StateFlow<List<AlphaSignal>> = alphaEngine.signals.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        val isPro: StateFlow<Boolean> = subscription.isPro.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            false
        )

        private val _isLoading = MutableStateFlow(true)
        val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

        // Правилни символи + намалено до 5 монети за да не се hit-ва rate limit
        private val TRACKED_COINS =
            listOf(
                Pair("bitcoin", "BTC"),
                Pair("ethereum", "ETH"),
                Pair("ripple", "XRP"),
                Pair("solana", "SOL"),
                Pair("cardano", "ADA"),
            )

        init {
            generateSignals()
        }

        fun generateSignals() {
            viewModelScope.launch {
                _isLoading.value = true
                val results = mutableListOf<CoinSignal>()

                // Последователно (не паралелно) — защита от CoinGecko rate limit
                for ((coinId, symbol) in TRACKED_COINS) {
                    try {
                        // Пауза между заявките за да не ударим rate limit
                        if (results.isNotEmpty()) delay(2500L)

                        val ohlc = getOHLCUseCase(coinId, 14).first()
                        if (ohlc.isNotEmpty()) {
                            val prices = ohlc.map { it.close }
                            val rsi = taEngine.calculateRSI(prices)
                            val macd = taEngine.calculateMACD(prices)

                            val indicators = mutableListOf<IndicatorStatus>()
                            var bullish = 0
                            var bearish = 0
                            var neutral = 0

                            // RSI сигнал
                            val rsiSent =
                                when {
                                    rsi < 30 -> Sentiment.BULLISH.also { bullish++ }
                                    rsi > 70 -> Sentiment.BEARISH.also { bearish++ }
                                    else -> Sentiment.NEUTRAL.also { neutral++ }
                                }
                            indicators.add(
                                IndicatorStatus("RSI", String.format("%.1f", rsi), rsiSent),
                            )

                            // MACD сигнал
                            val macdHist = macd.histogram.lastOrNull() ?: 0.0
                            val macdSent =
                                if (macdHist > 0) {
                                    Sentiment.BULLISH.also { bullish++ }
                                } else {
                                    Sentiment.BEARISH.also { bearish++ }
                                }
                            indicators.add(
                                IndicatorStatus(
                                    "MACD",
                                    if (macdHist > 0) "BULL" else "BEAR",
                                    macdSent,
                                ),
                            )

                            // EMA сигнал (ако последната цена е над EMA50 → bullish)
                            val ema50 = taEngine.calculateEMA(prices, 50).lastOrNull()
                            if (ema50 != null) {
                                val emaSent =
                                    if (prices.last() > ema50) {
                                        Sentiment.BULLISH.also { bullish++ }
                                    } else {
                                        Sentiment.BEARISH.also { bearish++ }
                                    }
                                indicators.add(
                                    IndicatorStatus(
                                        "EMA50",
                                        if (prices.last() > ema50) "ABOVE" else "BELOW",
                                        emaSent,
                                    ),
                                )
                            }

                            val strength =
                                when {
                                    bullish > bearish + 1 -> SignalStrength.BUY
                                    bearish > bullish + 1 -> SignalStrength.SELL
                                    else -> SignalStrength.NEUTRAL
                                }

                            val confidence = (bullish.toFloat() / (bullish + bearish + neutral).coerceAtLeast(1))
                            val composite =
                                CompositeSignal(
                                    strength,
                                    bullish,
                                    bearish,
                                    neutral,
                                    indicators,
                                    confidence,
                                )
                            val currentPrice = ohlc.last().close

                            results.add(CoinSignal(coinId, symbol, currentPrice, composite))

                            // Обнови UI след всяка монета — не чакай всичките
                            _signals.value =
                                results
                                    .toList()
                                    .sortedByDescending { it.signal.bullishCount }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("CryptoDept_Signals", "Error for $coinId: ${e.message}")
                    }
                }

                _isLoading.value = false
            }
        }
    }
