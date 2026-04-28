package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.OHLCData
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class TechnicalAnalysisEngine @Inject constructor() {

    enum class CandlePattern {
        BULLISH_ENGULFING, BEARISH_ENGULFING, DOJI, HAMMER, 
        SHOOTING_STAR, MORNING_STAR, EVENING_STAR, THREE_WHITE_SOLDIERS
    }

    data class PatternDetection(
        val pattern: CandlePattern,
        val isBullish: Boolean,
        val description: String
    )

    fun detectPatterns(ohlc: List<OHLCData>): List<PatternDetection> {
        if (ohlc.size < 3) return emptyList()
        val detected = mutableListOf<PatternDetection>()
        val last = ohlc.last()
        val prev = ohlc[ohlc.size - 2]

        if (last.bodySize <= last.totalSize * 0.05) {
            detected.add(PatternDetection(CandlePattern.DOJI, true, "Indecision in the market."))
        }
        if (last.lowerWick >= last.bodySize * 2 && last.upperWick <= last.bodySize) {
            detected.add(PatternDetection(CandlePattern.HAMMER, true, "Buyers rejected lower prices."))
        }
        if (last.upperWick >= last.bodySize * 2 && last.lowerWick <= last.bodySize) {
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

    fun calculateRSI(prices: List<Double>, period: Int = 14): Double {
        if (prices.size <= period) return 50.0
        val changes = prices.zipWithNext { a, b -> b - a }
        var avgGain = changes.take(period).filter { it > 0 }.sum() / period
        var avgLoss = changes.take(period).filter { it < 0 }.map { kotlin.math.abs(it) }.sum() / period

        for (i in period until changes.size) {
            val change = changes[i]
            val gain = if (change > 0) change else 0.0
            val loss = if (change < 0) kotlin.math.abs(change) else 0.0
            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period
        }
        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }

    fun calculateEMA(prices: List<Double>, period: Int): List<Double> {
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
        val ema12 = calculateEMA(prices, 12)
        val ema26 = calculateEMA(prices, 26)
        val macdLine = if (ema12.size >= ema26.size) {
            ema12.takeLast(ema26.size).zip(ema26) { a, b -> a - b }
        } else emptyList()
        val signalLine = calculateEMA(macdLine, 9)
        val histogram = if (macdLine.size >= signalLine.size) {
            macdLine.takeLast(signalLine.size).zip(signalLine) { m, s -> m - s }
        } else emptyList()
        return MACDResult(macdLine, signalLine, histogram)
    }

    fun calculateBollingerBands(prices: List<Double>, period: Int = 20, deviation: Double = 2.0): BollingerResult {
        val lastPrices = prices.takeLast(period)
        val middle = if (lastPrices.isNotEmpty()) lastPrices.average() else 0.0
        val variance = if (lastPrices.isNotEmpty()) lastPrices.map { (it - middle) * (it - middle) }.average() else 0.0
        val stdDev = sqrt(variance)
        return BollingerResult(middle + (deviation * stdDev), middle, middle - (deviation * stdDev))
    }

    fun calculateFibonacciLevels(high: Double, low: Double): Map<String, Double> {
        val diff = high - low
        return mapOf(
            "0%" to high, "23.6%" to high - (diff * 0.236), "38.2%" to high - (diff * 0.382),
            "50%" to high - (diff * 0.5), "61.8%" to high - (diff * 0.618),
            "78.6%" to high - (diff * 0.786), "100%" to low
        )
    }

    fun calculateOBV(ohlc: List<OHLCData>): Double {
        var currentObv = 0.0
        for (i in 1 until ohlc.size) {
            val curr = ohlc[i]
            val prev = ohlc[i - 1]
            if (curr.close > prev.close) currentObv += curr.volume
            else if (curr.close < prev.close) currentObv -= curr.volume
        }
        return currentObv
    }
}

data class MACDResult(val macdLine: List<Double>, val signalLine: List<Double>, val histogram: List<Double>)
data class BollingerResult(val upper: Double, val middle: Double, val lower: Double) {
    val bandWidth: Double get() = if (middle != 0.0) (upper - lower) / middle else 0.0
}
