package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.*
import com.cryptodept.domain.repository.DerivativesRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OptimalEntryCalculator @Inject constructor(
    private val taEngine: TechnicalAnalysisEngine,
    private val derivativesRepository: DerivativesRepository
) {
    suspend fun analyze(
        coin: String,
        currentPrice: Double,
        ohlcData: List<OHLCData>,
        rsi: Double,
        bollingerUpper: Double,
        bollingerLower: Double,
        bollingerMid: Double
    ): EntryAnalysis {

        val fundingRateResult = derivativesRepository.getFundingRate(coin.uppercase())
        val fundingRate = fundingRateResult.getOrNull()?.binanceRate ?: 0.0
            
        val liquidationLevelsResult = derivativesRepository.getLiquidationData(coin.uppercase())
        val liquidationLevels = liquidationLevelsResult.getOrNull()?.heatmapLevels ?: emptyList()

        val prices = ohlcData.map { it.close }
        val ema50 = taEngine.calculateEMA(prices, minOf(50, prices.size)).lastOrNull() ?: currentPrice
        val ema200 = taEngine.calculateEMA(prices, minOf(200, prices.size)).lastOrNull() ?: currentPrice
        val fibLevels = calculateFibLevels(ohlcData)

        var score = 50 // Начален неутрален score
        val whyNotNow = mutableListOf<String>()

        // RSI анализ
        if (rsi > 70) { score -= 25; whyNotNow.add("RSI: ${String.format(java.util.Locale.US, "%.1f", rsi)} — Overbought zone") }
        else if (rsi > 60) { score -= 10; whyNotNow.add("RSI: ${String.format(java.util.Locale.US, "%.1f", rsi)} — Elevated") }
        else if (rsi < 35) { score += 20 }

        // Bollinger Bands позиция
        val bbRange = bollingerUpper - bollingerLower
        val bbPosition = if (bbRange > 0) (currentPrice - bollingerLower) / bbRange else 0.5
        if (bbPosition > 0.85) { score -= 20; whyNotNow.add("Price at Bollinger Upper Band") }
        else if (bbPosition < 0.15) { score += 15 }

        // Funding Rate
        if (fundingRate > 0.08) { score -= 20; whyNotNow.add("Funding: ${String.format(java.util.Locale.US, "%.4f", fundingRate)}% — Longs overlevered") }
        else if (fundingRate < -0.02) { score += 10 }

        // Значими ликвидационни нива над текущата цена
        val nearLiqLevel = liquidationLevels.filter {
            it.price > currentPrice && it.price < currentPrice * 1.05 && it.isSignificant
        }.maxByOrNull { it.longLiquidationUsd + it.shortLiquidationUsd }

        nearLiqLevel?.let {
            score -= 15
            val totalMil = (it.longLiquidationUsd + it.shortLiquidationUsd) / 1_000_000
            whyNotNow.add("$${String.format(java.util.Locale.US, "%.0f", totalMil)}M liquidation cluster at $${String.format(java.util.Locale.US, "%,.0f", it.price)}")
        }

        // EMA позиция
        if (currentPrice > ema50 * 1.05) { score -= 10; whyNotNow.add("Price extended above EMA50") }

        // По-добри зони
        val betterZones = mutableListOf<EntryZone>()

        // Fibonacci 38.2% и 61.8%
        fibLevels["38.2%"]?.let { fib382 ->
            if (fib382 < currentPrice * 0.98) {
                betterZones.add(EntryZone(
                    ZoneType.IDEAL,
                    fib382 * 0.99, fib382 * 1.01,
                    "Fibonacci 38.2% retracement level",
                    35.0, 3
                ))
            }
        }

        // EMA50 като динамична подкрепа
        if (ema50 < currentPrice * 0.97) {
            betterZones.add(EntryZone(
                ZoneType.DECENT,
                ema50 * 0.995, ema50 * 1.005,
                "EMA50 dynamic support",
                42.0, 2
            ))
        }

        // Bollinger Mid като реверсия цел
        if (bbPosition > 0.7) {
            betterZones.add(EntryZone(
                ZoneType.DECENT,
                bollingerMid * 0.995, bollingerMid * 1.005,
                "Bollinger Band middle (mean reversion target)",
                50.0, 2
            ))
        }

        val finalScore = score.coerceIn(0, 100)
        val verdict = when {
            finalScore >= 75 -> EntryVerdict.EXCELLENT_NOW
            finalScore >= 50 -> EntryVerdict.ACCEPTABLE_NOW
            finalScore >= 30 -> EntryVerdict.WAIT
            else             -> EntryVerdict.AVOID_NOW
        }

        return EntryAnalysis(
            coin, currentPrice, finalScore, verdict, whyNotNow,
            betterZones.sortedBy { it.priceFrom },
            betterZones.isNotEmpty() && finalScore < 50
        )
    }

    private fun calculateFibLevels(candles: List<OHLCData>): Map<String, Double> {
        val recent = candles.takeLast(50)
        if (recent.isEmpty()) return emptyMap()
        val high = recent.maxOf { it.high }
        val low = recent.minOf { it.low }
        val diff = high - low
        return mapOf(
            "0%"    to high,
            "23.6%" to high - diff * 0.236,
            "38.2%" to high - diff * 0.382,
            "50%"   to high - diff * 0.500,
            "61.8%" to high - diff * 0.618,
            "78.6%" to high - diff * 0.786,
            "100%"  to low
        )
    }
}
