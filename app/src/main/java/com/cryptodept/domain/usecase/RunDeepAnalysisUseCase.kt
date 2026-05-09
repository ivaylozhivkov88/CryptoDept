package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.*
import com.cryptodept.domain.repository.ChartRepository
import com.cryptodept.domain.repository.CryptoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import javax.inject.Inject

class RunDeepAnalysisUseCase
    @Inject
    constructor(
        private val cryptoRepository: CryptoRepository,
        private val chartRepository: ChartRepository,
        private val taEngine: TechnicalAnalysisEngine,
        private val sentimentAnalyzer: SentimentAnalyzer,
    ) {
        suspend fun execute(
            coinId: String,
            days: Int,
        ): Result<DeepAnalysisResult> =
            withContext(Dispatchers.Default) {
                try {
                    val normalizedId =
                        when (coinId.lowercase()) {
                            "btc" -> "bitcoin"
                            "eth" -> "ethereum"
                            else -> coinId.lowercase()
                        }

                    withTimeoutOrNull(5000) {
                        chartRepository.refreshOHLCData(normalizedId, days)
                    }

                    val ohlcData = cryptoRepository.getOHLCData(normalizedId, days)

                    if (ohlcData.isEmpty()) {
                        return@withContext Result.failure(Exception("NO DATA FOR $normalizedId"))
                    }

                    val prices = ohlcData.map { it.close }
                    val rsi = taEngine.calculateRSI(prices)
                    val macd = taEngine.calculateMACD(prices)
                    val patterns = taEngine.detectPatterns(ohlcData)
                    val fib = taEngine.calculateFibonacciLevels(prices.maxOrNull() ?: 0.0, prices.minOrNull() ?: 0.0)

                    val compositeSignal = calculateCompositeSignal(rsi, macd, patterns)
                    val sentimentResult = sentimentAnalyzer.analyzeCoin(coinId.uppercase())

                    val traces = generateTraces(normalizedId, prices, ohlcData, rsi, macd)

                    Result.success(
                        DeepAnalysisResult(
                            coinId = coinId.uppercase(),
                            compositeSignal = compositeSignal,
                            currentPrice = cryptoRepository.getCachedPrice(normalizedId),
                            ohlcData = ohlcData,
                            patterns = patterns,
                            fibonacci = fib,
                            rsiValue = rsi,
                            sentiment = sentimentResult,
                            traces = traces,
                        ),
                    )
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        private fun calculateCompositeSignal(
            rsi: Double,
            macd: MACDResult,
            patterns: List<TechnicalAnalysisEngine.PatternDetection>
        ): CompositeSignal {
            val indicators = mutableListOf<IndicatorStatus>()
            var bullish = 0
            var bearish = 0
            var neutral = 0

            // 1. RSI Signal
            val rsiSent = getRsiSentiment(rsi)
            when (rsiSent) {
                Sentiment.BULLISH -> bullish++
                Sentiment.BEARISH -> bearish++
                else -> neutral++
            }
            indicators.add(IndicatorStatus("RSI", String.format(Locale.US, "%.1f", rsi), rsiSent))

            // 2. MACD Signal
            val macdHist = macd.histogram.lastOrNull() ?: 0.0
            val macdSent = if (macdHist > 0) Sentiment.BULLISH.also { bullish++ } else Sentiment.BEARISH.also { bearish++ }
            indicators.add(IndicatorStatus("MACD", if (macdHist > 0) "BULL" else "BEAR", macdSent))

            // 3. Patterns Signal
            patterns.forEach { p -> if (p.isBullish) bullish += 2 else bearish += 2 }

            val totalSignals = (bullish + bearish + neutral).coerceAtLeast(1)
            val confidence = (bullish.coerceAtLeast(bearish).toFloat() / totalSignals).coerceIn(0.42f, 0.96f)
            val strength = calculateStrength(bullish, bearish, totalSignals)

            return CompositeSignal(strength, bullish, bearish, neutral, indicators, confidence)
        }

        private fun getRsiSentiment(rsi: Double): Sentiment = when {
            rsi < 35 -> Sentiment.BULLISH
            rsi > 65 -> Sentiment.BEARISH
            else -> Sentiment.NEUTRAL
        }

        private fun calculateStrength(bullish: Int, bearish: Int, total: Int): SignalStrength = when {
            bullish >= total * 0.65 -> SignalStrength.STRONG_BUY
            bearish >= total * 0.65 -> SignalStrength.STRONG_SELL
            bullish > bearish -> SignalStrength.BUY
            bearish > bullish -> SignalStrength.SELL
            else -> SignalStrength.NEUTRAL
        }

        private suspend fun generateTraces(
            coinId: String,
            prices: List<Double>,
            ohlc: List<OHLCData>,
            rsi: Double,
            macd: MACDResult
        ): List<AnalysisTrace> {
            val macdHist = macd.histogram.lastOrNull() ?: 0.0
            val lastOhlc = ohlc.last()
            val avgVolume = ohlc.map { it.volume }.average().coerceAtLeast(1.0)
            
            return taEngine.generateTechnicalTrace(
                rsi = rsi,
                macdHistogram = macdHist,
                price = cryptoRepository.getCachedPrice(coinId),
                ema50 = taEngine.calculateEMA(prices, 50).lastOrNull() ?: 0.0,
                ema200 = taEngine.calculateEMA(prices, 200).lastOrNull() ?: 0.0,
                volumeMultiplier = lastOhlc.volume / avgVolume,
            )
        }
    }

data class DeepAnalysisResult(
    val coinId: String,
    val compositeSignal: CompositeSignal,
    val currentPrice: Double,
    val ohlcData: List<OHLCData>,
    val patterns: List<TechnicalAnalysisEngine.PatternDetection>,
    val fibonacci: Map<String, Double>,
    val rsiValue: Double,
    val sentiment: SentimentResult?,
    val traces: List<AnalysisTrace> = emptyList(),
)
