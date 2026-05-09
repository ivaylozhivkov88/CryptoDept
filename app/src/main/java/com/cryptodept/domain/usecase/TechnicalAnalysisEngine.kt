package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.AnalysisTrace
import com.cryptodept.domain.model.MACDIndicator
import com.cryptodept.domain.model.OHLCData
import com.cryptodept.domain.model.RSIIndicator
import com.cryptodept.domain.model.TechnicalSnapshot
import com.cryptodept.domain.model.TraceIntensity
import com.cryptodept.domain.model.VolumeIndicator
import com.cryptodept.util.AppConstants
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class TechnicalAnalysisEngine
    @Inject
    constructor() {
        fun buildSnapshot(
            symbol: String,
            ohlc: List<OHLCData>,
        ): TechnicalSnapshot {
            val prices = ohlc.map { it.close }
            val rsi = calculateRSI(prices)
            val macd = calculateMACD(prices)
            val lastVol = ohlc.lastOrNull()?.volume ?: 0.0
            val avgVol = ohlc.map { it.volume }.average()
            val volMultiplier = if (avgVol > 0) lastVol / avgVol else 1.0
            val currentPrice = prices.lastOrNull() ?: 0.0

            return TechnicalSnapshot(
                coinSymbol = symbol,
                rsi = RSIIndicator(rsi.toFloat()),
                macd =
                    MACDIndicator(
                        macdLine = (macd.macdLine.lastOrNull() ?: 0.0).toFloat(),
                        signalLine = (macd.signalLine.lastOrNull() ?: 0.0).toFloat(),
                        histogram = (macd.histogram.lastOrNull() ?: 0.0).toFloat(),
                    ),
                volume =
                    VolumeIndicator(
                        volumeChange24h = 0.0,
                        volumeAvg14 = avgVol,
                        currentVolume = lastVol,
                        volumeMultiplier = volMultiplier,
                    ),
                price = currentPrice,
                priceChange24h = 0.0,
            )
        }

        fun generateTechnicalTrace(
            rsi: Double,
            macdHistogram: Double,
            price: Double,
            ema50: Double,
            ema200: Double,
            volumeMultiplier: Double,
        ): List<AnalysisTrace> {
            return listOfNotNull(
                createRSITrace(rsi),
                createMACDTrace(macdHistogram),
                createTrendTrace(price, ema50, ema200),
                createVolumeTrace(volumeMultiplier)
            )
        }

        private fun createRSITrace(rsi: Double): AnalysisTrace {
            val formattedRsi = String.format(Locale.US, "%.1f", rsi)
            return when {
                rsi >= AppConstants.TA.RSI_OVERBOUGHT ->
                    AnalysisTrace(
                        "RSI",
                        AppConstants.TA.Scores.RSI_HIGH,
                        "OVERBOUGHT",
                        "RSI at $formattedRsi indicates extreme buying pressure. High risk of immediate reversal.",
                        TraceIntensity.HIGH,
                    )
                rsi <= AppConstants.TA.RSI_OVERSOLD ->
                    AnalysisTrace(
                        "RSI",
                        AppConstants.TA.Scores.RSI_LOW,
                        "OVERSOLD",
                        "RSI at $formattedRsi suggests sellers are exhausted. Potential bounce imminent.",
                        TraceIntensity.HIGH,
                    )
                else ->
                    AnalysisTrace(
                        "RSI",
                        AppConstants.TA.Scores.RSI_MID,
                        "NEUTRAL",
                        "RSI is in the middle range, suggesting balanced market momentum.",
                        TraceIntensity.LOW,
                    )
            }
        }

        private fun createMACDTrace(macdHistogram: Double): AnalysisTrace {
            return when {
                macdHistogram > 0 ->
                    AnalysisTrace(
                        "MACD",
                        AppConstants.TA.Scores.MACD_BULLISH,
                        "BULLISH",
                        "Positive histogram confirms upward momentum is building.",
                        TraceIntensity.MEDIUM,
                    )
                macdHistogram < 0 ->
                    AnalysisTrace(
                        "MACD",
                        AppConstants.TA.Scores.MACD_BEARISH,
                        "BEARISH",
                        "Negative histogram shows downward pressure is dominating.",
                        TraceIntensity.MEDIUM,
                    )
                else -> AnalysisTrace("MACD", AppConstants.TA.Scores.MACD_FLAT, "FLAT", "No clear MACD trend detected.", TraceIntensity.LOW)
            }
        }

        private fun createTrendTrace(price: Double, ema50: Double, ema200: Double): AnalysisTrace {
            return when {
                price > ema50 && ema50 > ema200 ->
                    AnalysisTrace(
                        "TREND",
                        AppConstants.TA.Scores.TREND_STRONG_BULLISH,
                        "STRONG BULLISH",
                        "Price is above key EMAs, and short-term EMA is above long-term EMA. Strong uptrend.",
                        TraceIntensity.EXTREME,
                    )
                price < ema50 && ema50 < ema200 ->
                    AnalysisTrace(
                        "TREND",
                        AppConstants.TA.Scores.TREND_STRONG_BEARISH,
                        "STRONG BEARISH",
                        "Price is below major averages. Market structure is severely damaged.",
                        TraceIntensity.EXTREME,
                    )
                price > ema200 ->
                    AnalysisTrace(
                        "TREND",
                        AppConstants.TA.Scores.TREND_BULLISH_BIAS,
                        "BULLISH BIAS",
                        "Price holding above 200 EMA indicates long-term support is intact.",
                        TraceIntensity.MEDIUM,
                    )
                else ->
                    AnalysisTrace(
                        "TREND",
                        AppConstants.TA.Scores.TREND_BEARISH_BIAS,
                        "BEARISH BIAS",
                        "Price trading below long-term averages. Caution advised.",
                        TraceIntensity.MEDIUM,
                    )
            }
        }

        private fun createVolumeTrace(volumeMultiplier: Double): AnalysisTrace {
            val formattedVol = String.format(Locale.US, "%.1f", volumeMultiplier)
            return when {
                volumeMultiplier > AppConstants.TA.VOL_SPIKE_THRESHOLD ->
                    AnalysisTrace(
                        "VOLUME",
                        AppConstants.TA.Scores.VOL_SPIKE,
                        "SPIKE",
                        "Unusually high volume (${formattedVol}x) confirms current move conviction.",
                        TraceIntensity.HIGH,
                    )
                volumeMultiplier < AppConstants.TA.VOL_DRY_THRESHOLD ->
                    AnalysisTrace(
                        "VOLUME",
                        AppConstants.TA.Scores.VOL_DRY,
                        "DRY",
                        "Low volume suggests a lack of interest at current levels.",
                        TraceIntensity.MEDIUM,
                    )
                else -> AnalysisTrace("VOLUME", AppConstants.TA.Scores.VOL_NORMAL, "NORMAL", "Trading volume is within average parameters.", TraceIntensity.LOW)
            }
        }

        enum class CandlePattern {
            BULLISH_ENGULFING,
            BEARISH_ENGULFING,
            DOJI,
            HAMMER,
            SHOOTING_STAR,
            MORNING_STAR,
            EVENING_STAR,
            THREE_WHITE_SOLDIERS,
        }

        data class PatternDetection(
            val pattern: CandlePattern,
            val isBullish: Boolean,
            val description: String,
        )

        fun detectPatterns(ohlc: List<OHLCData>): List<PatternDetection> {
            if (ohlc.size < 3) return emptyList()
            val detected = mutableListOf<PatternDetection>()
            val last = ohlc.last()
            val prev = ohlc[ohlc.size - 2]

            if (last.bodySize <= last.totalSize * AppConstants.TA.DOJI_BODY_RATIO) {
                detected.add(PatternDetection(CandlePattern.DOJI, true, "Indecision in the market."))
            }
            if (last.lowerWick >= last.bodySize * AppConstants.TA.HAMMER_WICK_RATIO && last.upperWick <= last.bodySize) {
                detected.add(PatternDetection(CandlePattern.HAMMER, true, "Buyers rejected lower prices."))
            }
            if (last.upperWick >= last.bodySize * AppConstants.TA.HAMMER_WICK_RATIO && last.lowerWick <= last.bodySize) {
                detected.add(PatternDetection(CandlePattern.SHOOTING_STAR, false, "Sellers rejected higher prices."))
            }
            if (!prev.isGreen && last.isGreen && last.open <= prev.close && last.close >= prev.open) {
                detected.add(PatternDetection(CandlePattern.BULLISH_ENGULFING, true, "Strong reversal signal."))
            }
            if (prev.isGreen && !last.isGreen && last.open >= prev.close && last.close <= prev.open) {
                detected.add(PatternDetection(CandlePattern.BEARISH_ENGULFING, false, "Strong bearish reversal."))
            }
            return detected
        }

        fun calculateRSI(
            prices: List<Double>,
            period: Int = AppConstants.TA.RSI_PERIOD,
        ): Double {
            if (prices.size <= period) return AppConstants.TA.RSI_NEUTRAL
            val changes = prices.zipWithNext { a, b -> b - a }
            var avgGain = changes.take(period).filter { it > 0 }.sum() / period
            var avgLoss =
                changes
                    .take(period)
                    .filter { it < 0 }
                    .map { kotlin.math.abs(it) }
                    .sum() / period

            for (i in period until changes.size) {
                val change = changes[i]
                val gain = if (change > 0) change else 0.0
                val loss = if (change < 0) kotlin.math.abs(change) else 0.0
                avgGain = (avgGain * (period - 1) + gain) / period
                avgLoss = (avgLoss * (period - 1) + loss) / period
            }
            
            return if (avgLoss == 0.0) {
                100.0
            } else {
                val rs = avgGain / avgLoss
                100.0 - (100.0 / (1.0 + rs))
            }
        }

        fun calculateEMA(
            prices: List<Double>,
            period: Int,
        ): List<Double> {
            if (prices.isEmpty()) return emptyList()
            val emaList = mutableListOf<Double>()
            val multiplier = 2.0 / (period + 1)
            var currentEma = prices.take(period).average()
            emaList.add(currentEma)
            for (i in period until prices.size) {
                currentEma = (prices[i] - currentEma) * multiplier + currentEma
                emaList.add(currentEma)
            }
            return emaList
        }

        fun calculateMACD(prices: List<Double>): MACDResult {
            val emaFast = calculateEMA(prices, AppConstants.TA.MACD_FAST)
            val emaSlow = calculateEMA(prices, AppConstants.TA.MACD_SLOW)
            val macdLine =
                if (emaFast.size >= emaSlow.size) {
                    emaFast.takeLast(emaSlow.size).zip(emaSlow) { a, b -> a - b }
                } else {
                    emptyList()
                }
            val signalLine = calculateEMA(macdLine, AppConstants.TA.MACD_SIGNAL)
            val histogram =
                if (macdLine.size >= signalLine.size) {
                    macdLine.takeLast(signalLine.size).zip(signalLine) { m, s -> m - s }
                } else {
                    emptyList()
                }
            return MACDResult(macdLine, signalLine, histogram)
        }

        fun calculateBollingerBands(
            prices: List<Double>,
            period: Int = AppConstants.TA.BOLLINGER_PERIOD,
            deviation: Double = AppConstants.TA.BOLLINGER_DEVIATION,
        ): BollingerResult {
            val lastPrices = prices.takeLast(period)
            val middle = if (lastPrices.isNotEmpty()) lastPrices.average() else 0.0
            val variance = if (lastPrices.isNotEmpty()) lastPrices.map { (it - middle) * (it - middle) }.average() else 0.0
            val stdDev = sqrt(variance)
            return BollingerResult(middle + (deviation * stdDev), middle, middle - (deviation * stdDev))
        }

        fun calculateFibonacciLevels(
            high: Double,
            low: Double,
        ): Map<String, Double> {
            val diff = high - low
            return mapOf(
                "0%" to high,
                "23.6%" to high - (diff * AppConstants.TA.Fibonacci.LEVEL_236),
                "38.2%" to high - (diff * AppConstants.TA.Fibonacci.LEVEL_382),
                "50%" to high - (diff * AppConstants.TA.Fibonacci.LEVEL_500),
                "61.8%" to high - (diff * AppConstants.TA.Fibonacci.LEVEL_618),
                "78.6%" to high - (diff * AppConstants.TA.Fibonacci.LEVEL_786),
                "100%" to low,
            )
        }

        fun calculateOBV(ohlc: List<OHLCData>): Double {
            var currentObv = 0.0
            for (i in 1 until ohlc.size) {
                val curr = ohlc[i]
                val prev = ohlc[i - 1]
                if (curr.close > prev.close) {
                    currentObv += curr.volume
                } else if (curr.close < prev.close) {
                    currentObv -= curr.volume
                }
            }
            return currentObv
        }
    }

data class MACDResult(
    val macdLine: List<Double>,
    val signalLine: List<Double>,
    val histogram: List<Double>,
)

data class BollingerResult(
    val upper: Double,
    val middle: Double,
    val lower: Double,
) {
    val bandWidth: Double get() = if (middle != 0.0) (upper - lower) / middle else 0.0
}
