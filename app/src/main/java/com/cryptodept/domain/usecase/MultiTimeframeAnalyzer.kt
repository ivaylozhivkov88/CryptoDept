package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.*
import com.cryptodept.domain.repository.ChartRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MultiTimeframeAnalyzer
    @Inject
    constructor(
        private val taEngine: TechnicalAnalysisEngine,
        private val chartRepository: ChartRepository,
        private val demoMode: com.cryptodept.util.DemoModeProvider,
    ) {
        // Mapping: timeframe label → CoinGecko days param
        private val TIMEFRAME_CONFIG =
            listOf(
                Triple("15m", 1, 96),
                Triple("1H", 1, 24),
                Triple("4H", 7, 42),
                Triple("1D", 30, 30),
                Triple("1W", 90, 13),
            )

        suspend fun analyze(coinId: String): MTFConsensus =
            withContext(Dispatchers.Default) {
                if (demoMode.isActive()) {
                    return@withContext MTFConsensus(
                        timeframes = listOf(
                            TimeframeSignal("15m", TrendDirection.UP, 62.0, MTFMacdSignal.BULLISH, EmaSignal.ABOVE_50, OverallSignal.BUY, 96),
                            TimeframeSignal("1H", TrendDirection.UP, 58.0, MTFMacdSignal.BULLISH, EmaSignal.ABOVE_50, OverallSignal.BUY, 24),
                            TimeframeSignal("4H", TrendDirection.SIDEWAYS, 54.0, MTFMacdSignal.NEUTRAL, EmaSignal.MIXED, OverallSignal.NEUTRAL, 42),
                            TimeframeSignal("1D", TrendDirection.DOWN, 42.0, MTFMacdSignal.BEARISH, EmaSignal.BELOW_50, OverallSignal.SELL, 30),
                            TimeframeSignal("1W", TrendDirection.DOWN, 38.0, MTFMacdSignal.BEARISH, EmaSignal.BELOW_ALL, OverallSignal.STRONG_SELL, 13)
                        ),
                        bullishCount = 2,
                        bearishCount = 2,
                        neutralCount = 1,
                        consensus = OverallSignal.NEUTRAL,
                        interpretation = "DEMO DATA: SHOWING MIXED BIAS ACROSS TIMEFRAMES.",
                        tradingBias = "NEUTRAL"
                    )
                }

                val timeframeSignals = mutableListOf<TimeframeSignal>()

                TIMEFRAME_CONFIG.forEach { (tfLabel, days, take) ->
                    try {
                        val allCandles = chartRepository.getOHLCData(coinId, days).first()
                        val candles = allCandles.takeLast(take)
                        if (candles.size < 14) return@forEach

                        val prices = candles.map { it.close }
                        val rsi = taEngine.calculateRSI(prices)
                        val macd = taEngine.calculateMACD(prices)

                        val macdHist = macd.histogram.lastOrNull() ?: 0.0
                        val macdSignal = when {
                            macdHist > 0 -> MTFMacdSignal.BULLISH
                            macdHist < 0 -> MTFMacdSignal.BEARISH
                            else -> MTFMacdSignal.NEUTRAL
                        }

                        val ema50 = taEngine.calculateEMA(prices, 50).lastOrNull() ?: 0.0
                        val ema200 = taEngine.calculateEMA(prices, 200).lastOrNull() ?: 0.0
                        val currentPrice = prices.last()

                        val emaSignal = when {
                            currentPrice > ema50 && currentPrice > ema200 -> EmaSignal.ABOVE_ALL
                            currentPrice > ema50 -> EmaSignal.ABOVE_50
                            currentPrice < ema50 && currentPrice < ema200 -> EmaSignal.BELOW_ALL
                            else -> EmaSignal.MIXED
                        }

                        val trend = when {
                            currentPrice > ema200 && prices[prices.size-1] > prices[prices.size-2] -> TrendDirection.UP
                            currentPrice < ema200 && prices[prices.size-1] < prices[prices.size-2] -> TrendDirection.DOWN
                            else -> TrendDirection.SIDEWAYS
                        }

                        val overall = calculateOverall(rsi, macdHist, currentPrice, ema50, trend)

                        timeframeSignals.add(
                            TimeframeSignal(tfLabel, trend, rsi, macdSignal, emaSignal, overall, candles.size)
                        )
                    } catch (e: Exception) {
                        // Skip failed timeframe
                    }
                }

                if (timeframeSignals.isEmpty()) throw Exception("NO_TIMEFRAME_DATA")

                val bullish = timeframeSignals.count { it.overallSignal == OverallSignal.BUY || it.overallSignal == OverallSignal.STRONG_BUY }
                val bearish = timeframeSignals.count { it.overallSignal == OverallSignal.SELL || it.overallSignal == OverallSignal.STRONG_SELL }
                val neutral = timeframeSignals.size - bullish - bearish

                val consensus = when {
                    bullish >= 4 -> OverallSignal.STRONG_BUY
                    bullish >= 3 -> OverallSignal.BUY
                    bearish >= 4 -> OverallSignal.STRONG_SELL
                    bearish >= 3 -> OverallSignal.SELL
                    else -> OverallSignal.NEUTRAL
                }

                MTFConsensus(
                    timeframes = timeframeSignals,
                    bullishCount = bullish,
                    bearishCount = bearish,
                    neutralCount = neutral,
                    consensus = consensus,
                    interpretation = generateInterpretation(consensus, bullish, bearish),
                    tradingBias = if (bullish > bearish) "BULLISH" else if (bearish > bullish) "BEARISH" else "NEUTRAL"
                )
            }

        private fun calculateOverall(rsi: Double, macdHist: Double, price: Double, ema50: Double, trend: TrendDirection): OverallSignal {
            var score = 0
            if (rsi < 35) score += 2
            if (rsi > 65) score -= 2
            if (macdHist > 0) score += 1 else score -= 1
            if (price > ema50) score += 1 else score -= 1
            if (trend == TrendDirection.UP) score += 1 else if (trend == TrendDirection.DOWN) score -= 1

            return when {
                score >= 3 -> OverallSignal.STRONG_BUY
                score >= 1 -> OverallSignal.BUY
                score <= -3 -> OverallSignal.STRONG_SELL
                score <= -1 -> OverallSignal.SELL
                else -> OverallSignal.NEUTRAL
            }
        }

        private fun generateInterpretation(consensus: OverallSignal, bullish: Int, bearish: Int): String {
            return "CONSENSUS IS ${consensus.name}. $bullish OF 5 TIMEFRAMES SHOW BULLISH BIAS, WHILE $bearish SHOW BEARISH PRESSURE."
        }
    }
