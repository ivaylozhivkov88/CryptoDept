package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.*
import com.cryptodept.domain.repository.ChartRepository
import com.cryptodept.domain.repository.CryptoRepository
import kotlinx.coroutines.*
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
        private val demoMode: com.cryptodept.util.DemoModeProvider,
    ) {
        suspend fun execute(
            coinId: String,
            days: Int,
        ): Result<DeepAnalysisResult> =
            withContext(Dispatchers.Default) {
                try {
                    if (demoMode.isActive()) {
                        val d = demoMode.getDemoAnalysis()
                        return@withContext Result.success(
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
                                        IndicatorStatus("EMA50", "SUPPORT", Sentiment.BULLISH)
                                    ),
                                    confidence = d.confidence
                                ),
                                currentPrice = d.currentPrice,
                                ohlcData = emptyList(),
                                patterns = emptyList(),
                                fibonacci = mapOf("0.618" to d.currentPrice * 0.98, "0.5" to d.currentPrice * 0.95),
                                rsiValue = d.rsi,
                                sentiment = SentimentResult(
                                    symbol = d.coinSymbol,
                                    verdict = SentimentVerdict.BULLISH,
                                    bullishPercent = 65,
                                    bearishPercent = 15,
                                    neutralPercent = 20,
                                    totalAnalyzed = 142
                                ),
                                traces = emptyList()
                            )
                        )
                    }

                    val normalizedId =
                        when (coinId.lowercase()) {
                            "btc" -> "bitcoin"
                            "eth" -> "ethereum"
                            else -> coinId.lowercase()
                        }

                    // 1. Fetch OHLC Data with overall timeout
                    val ohlcData = withTimeoutOrNull(12000) {
                        try {
                            chartRepository.refreshOHLCData(normalizedId, days)
                        } catch (_: Exception) {}
                        cryptoRepository.getOHLCData(normalizedId, days)
                    } ?: emptyList()

                    if (ohlcData.isEmpty()) {
                        return@withContext Result.failure(Exception("NO DATA FOR $normalizedId"))
                    }

                    // 2. Run Analysis in Parallel
                    val analysisJob = async {
                        val prices = ohlcData.map { it.close }
                        val rsi = taEngine.calculateRSI(prices)
                        val macd = taEngine.calculateMACD(prices)
                        val patterns = taEngine.detectPatterns(ohlcData)
                        val fib = taEngine.calculateFibonacciLevels(prices.maxOrNull() ?: 0.0, prices.minOrNull() ?: 0.0)
                        val compositeSignal = calculateCompositeSignal(rsi, macd, patterns)
                        val traces = generateTraces(normalizedId, prices, ohlcData, rsi, macd)
                        
                        Triple(compositeSignal, fib, traces) to Triple(rsi, macd, patterns)
                    }

                    val sentimentJob = async {
                        withTimeoutOrNull(8000) {
                            sentimentAnalyzer.analyzeCoin(coinId.uppercase())
                        }
                    }

                    val (analysisData, taData) = analysisJob.await()
                    val (compositeSignal, fib, traces) = analysisData
                    val (rsi, macd, patterns) = taData
                    val sentimentResult = sentimentJob.await()

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
