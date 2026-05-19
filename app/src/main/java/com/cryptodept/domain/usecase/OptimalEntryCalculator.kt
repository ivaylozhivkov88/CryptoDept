package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.*
import com.cryptodept.domain.repository.DerivativesRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OptimalEntryCalculator
    @Inject
    constructor(
        private val taEngine: TechnicalAnalysisEngine,
        private val derivativesRepository: DerivativesRepository,
    ) {
        suspend fun analyze(
            coin: String,
            currentPrice: Double,
            ohlcData: List<OHLCData>,
            rsi: Double,
            bollingerUpper: Double,
            bollingerLower: Double,
            bollingerMid: Double,
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
            if (rsi > 70) {
                score -= 25
                whyNotNow.add("RSI: ${String.format(java.util.Locale.US, "%.1f", rsi)} — Overbought zone")
            } else if (rsi > 60) {
                score -= 10
                whyNotNow.add("RSI: ${String.format(java.util.Locale.US, "%.1f", rsi)} — Elevated")
            } else if (rsi < 35) {
                score += 20
            }

            // Bollinger Bands позиция
            val bbRange = bollingerUpper - bollingerLower
            val bbPosition = if (bbRange > 0) (currentPrice - bollingerLower) / bbRange else 0.5
            if (bbPosition > 0.85) {
                score -= 20
                whyNotNow.add("Price at Bollinger Upper Band")
            } else if (bbPosition < 0.15) {
                score += 15
            }

            // Funding Rate
            if (fundingRate > 0.08) {
                score -= 20
                whyNotNow.add("Funding: ${String.format(java.util.Locale.US, "%.4f", fundingRate)}% — Longs overlevered")
            } else if (fundingRate < -0.02) {
                score += 10
            }

            // Значими ликвидационни нива над текущата цена
            val nearLiqLevel =
                liquidationLevels
                    .filter {
                        it.price > currentPrice && it.price < currentPrice * 1.05 && it.isSignificant
                    }.maxByOrNull { it.longLiquidationUsd + it.shortLiquidationUsd }

            nearLiqLevel?.let {
                score -= 15
                val totalMil = (it.longLiquidationUsd + it.shortLiquidationUsd) / 1_000_000
                whyNotNow.add(
                    "$${String.format(
                        java.util.Locale.US,
                        "%.0f",
                        totalMil,
                    )}M liquidation cluster at $${String.format(java.util.Locale.US, "%,.0f", it.price)}",
                )
            }

            // EMA позиция
            if (currentPrice > ema50 * 1.05) {
                score -= 10
                whyNotNow.add("Price extended above EMA50")
            }

            // Fibonacci levels for zones
            val fib382 = fibLevels["38.2%"] ?: (currentPrice * 0.95)
            val fib618 = fibLevels["61.8%"] ?: (currentPrice * 0.90)

            // 7. IDENTIFIED ENTRY ZONES (Task 2.6 - Uniform logic)
            val betterZones = mutableListOf<EntryZone>()

            // Zone 1: EMA Support (Decent)
            betterZones.add(
                EntryZone(
                    ZoneType.DECENT,
                    ema50 * 0.99,
                    ema50 * 1.01,
                    "EMA50 Dynamic Trend Support",
                    45.0,
                    2,
                ),
            )

            // Zone 2: Fibonacci Golden Ratio (Ideal)
            betterZones.add(
                EntryZone(
                    ZoneType.IDEAL,
                    fib618 * 0.99,
                    fib618 * 1.01,
                    "Fibonacci 61.8% Golden Ratio Cluster",
                    30.0,
                    3,
                ),
            )

            // Zone 3: Mean Reversion (Decent)
            betterZones.add(
                EntryZone(
                    ZoneType.DECENT,
                    bollingerMid * 0.995,
                    bollingerMid * 1.005,
                    "Bollinger Middle (Mean Reversion Target)",
                    50.0,
                    2,
                ),
            )

            val finalScore = score.coerceIn(0, 100)
            val verdict =
                when {
                    finalScore >= 75 -> EntryVerdict.EXCELLENT_NOW
                    finalScore >= 50 -> EntryVerdict.ACCEPTABLE_NOW
                    finalScore >= 30 -> EntryVerdict.WAIT
                    else -> EntryVerdict.AVOID_NOW
                }

            return EntryAnalysis(
                coin = coin,
                currentPrice = currentPrice,
                entryScore = finalScore,
                verdict = verdict,
                whyNotNow = whyNotNow,
                betterZones = betterZones.sortedBy { it.priceTo },
                immediateAlertSuggested = betterZones.isNotEmpty() && finalScore < 50,
            )
        }

        private fun calculateFibLevels(candles: List<OHLCData>): Map<String, Double> {
            val recent = candles.takeLast(50)
            if (recent.isEmpty()) return emptyMap()
            val high = recent.maxOf { it.high }
            val low = recent.minOf { it.low }
            val diff = high - low
            return mapOf(
                "0%" to high,
                "23.6%" to high - diff * 0.236,
                "38.2%" to high - diff * 0.382,
                "50%" to high - diff * 0.500,
                "61.8%" to high - diff * 0.618,
                "78.6%" to high - diff * 0.786,
                "100%" to low,
            )
        }
    }
