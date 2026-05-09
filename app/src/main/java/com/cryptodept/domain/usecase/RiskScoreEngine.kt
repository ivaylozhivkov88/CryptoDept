package com.cryptodept.domain.usecase

import com.cryptodept.util.AppConstants
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RiskScoreEngine
    @Inject
    constructor() {
        private val _currentScore = kotlinx.coroutines.flow.MutableStateFlow<Int>(50)
        val currentScore = _currentScore.asStateFlow()

        fun observeRiskScore() = currentScore

        data class RiskScore(
            val overall: Int, // 0-100 (0=minimal risk, 100=maximum)
            val level: RiskLevel,
            val components: List<RiskComponent>,
            val dominantFactors: List<String>, // Top 3 factors
            val recommendation: String,
            val calculatedAt: Long,
        )

        data class RiskComponent(
            val name: String,
            val score: Int, // 0-100
            val weight: Float, // Weight in overall score
            val signal: String, // Description
            val isBearish: Boolean,
        )

        enum class RiskLevel(
            val label: String,
            val color: Long,
        ) {
            VERY_LOW("MINIMAL RISK", 0xFF00FF41), // < 20
            LOW("LOW RISK", 0xFF39FF14), // 20-40
            MODERATE("MODERATE RISK", 0xFFFFB000), // 40-60
            HIGH("HIGH RISK", 0xFFFF6600), // 60-80
            EXTREME("EXTREME RISK", 0xFFFF3B30), // > 80
        }

        fun calculate(
            rsi: Double,
            fundingRate: Double, // % (e.g. 0.08 = 0.08%)
            longShortRatio: Double, // > 1.0 = more longs
            fearGreedIndex: Int, // 0-100
            exchangeInflowChange: Double, // % change (positive = more entering exchange)
            openInterestChange: Double, // % change 24h
            priceChange24h: Double, // % price change
            macroRisk: Double = 0.5, // 0.0-1.0 (from DXY + S&P correlation)
        ): RiskScore {
            val components = mutableListOf<RiskComponent>()

            // --- RSI component (weight: 15%) ---
            val rsiScore =
                when {
                    rsi > AppConstants.Risk.RSI_EXTREME_RISK -> 90
                    rsi > AppConstants.Risk.RSI_HIGH_RISK -> 70
                    rsi > AppConstants.Risk.RSI_ELEVATED_RISK -> 40
                    rsi in 40.0..60.0 -> 20
                    rsi < AppConstants.TA.RSI_OVERSOLD -> 15 // Oversold = low risk for new short
                    else -> 30
                }
            components.add(
                RiskComponent(
                    "RSI",
                    rsiScore,
                    0.15f,
                    "RSI: ${String.format(Locale.US, "%.1f", rsi)} — ${if (rsi > AppConstants.Risk.RSI_HIGH_RISK) {
                        "OVERBOUGHT"
                    } else if (rsi < AppConstants.TA.RSI_OVERSOLD) {
                        "OVERSOLD"
                    } else {
                        "NEUTRAL"
                    }}",
                    rsi > AppConstants.Risk.RSI_HIGH_RISK,
                ),
            )

            // --- Funding Rate component (weight: 20%) ---
            val fundingScore =
                when {
                    fundingRate > AppConstants.Risk.FUNDING_EXTREME -> 95 // Extreme — crash risk
                    fundingRate > AppConstants.Risk.FUNDING_HIGH -> 75 // High
                    fundingRate > AppConstants.Risk.FUNDING_ELEVATED -> 40 // Elevated
                    fundingRate in -0.02..0.02 -> 20 // Normal
                    fundingRate < -0.05 -> 10 // Negative = bears paying = low crash risk
                    else -> 30
                }
            val fundingPct = String.format(Locale.US, "%.4f", fundingRate)
            components.add(
                RiskComponent(
                    "FUNDING RATE",
                    fundingScore,
                    0.20f,
                    "Rate: $fundingPct% — ${when {
                        fundingRate > AppConstants.Risk.FUNDING_HIGH -> "ELEVATED (longs overlevered)"
                        fundingRate < -0.02 -> "NEGATIVE (shorts overlevered)"
                        else -> "NORMAL"
                    }}",
                    fundingRate > AppConstants.Risk.FUNDING_HIGH,
                ),
            )

            // --- Long/Short Ratio (weight: 15%) ---
            val lsScore =
                when {
                    longShortRatio > AppConstants.Risk.LS_RATIO_EXTREME -> 85 // Extreme longs
                    longShortRatio > AppConstants.Risk.LS_RATIO_HIGH -> 65
                    longShortRatio > AppConstants.Risk.LS_RATIO_ELEVATED -> 45
                    longShortRatio in 0.8..1.5 -> 20
                    longShortRatio < 0.5 -> 15 // Extreme shorts = contrarian bullish
                    else -> 30
                }
            components.add(
                RiskComponent(
                    "LONG/SHORT",
                    lsScore,
                    0.15f,
                    "Ratio: ${String.format(Locale.US, "%.2f", longShortRatio)} — ${if (longShortRatio > AppConstants.Risk.LS_RATIO_HIGH) {
                        "CROWDED LONGS"
                    } else if (longShortRatio < 0.7) {
                        "CROWDED SHORTS"
                    } else {
                        "BALANCED"
                    }}",
                    longShortRatio > AppConstants.Risk.LS_RATIO_HIGH,
                ),
            )

            // --- Fear & Greed (weight: 10%) ---
            val fgScore =
                when {
                    fearGreedIndex > AppConstants.Risk.FG_EXTREME_GREED -> 90 // Extreme Greed
                    fearGreedIndex > AppConstants.Risk.FG_GREED -> 65
                    fearGreedIndex in AppConstants.Risk.FG_NEUTRAL_MIN..AppConstants.Risk.FG_GREED -> 30
                    fearGreedIndex < AppConstants.Risk.FG_FEAR_MAX -> 10 // Extreme Fear = buy signal
                    else -> 25
                }
            components.add(
                RiskComponent(
                    "FEAR & GREED",
                    fgScore,
                    0.10f,
                    "Index: $fearGreedIndex — ${when {
                        fearGreedIndex > AppConstants.Risk.FG_GREED + 5 -> "EXTREME GREED"
                        fearGreedIndex > AppConstants.Risk.FG_GREED - 15 -> "GREED"
                        fearGreedIndex < AppConstants.Risk.FG_FEAR_MAX -> "EXTREME FEAR"
                        fearGreedIndex < AppConstants.Risk.FG_NEUTRAL_MIN -> "FEAR"
                        else -> "NEUTRAL"
                    }}",
                    fearGreedIndex > AppConstants.Risk.FG_GREED,
                ),
            )

            // --- Exchange Inflows (weight: 20%) ---
            val inflowScore =
                when {
                    exchangeInflowChange > AppConstants.Risk.INFLOW_MASSIVE -> 90 // Massive inflows to exchanges
                    exchangeInflowChange > AppConstants.Risk.INFLOW_HIGH -> 70
                    exchangeInflowChange > AppConstants.Risk.INFLOW_ELEVATED -> 40
                    exchangeInflowChange in -5.0..5.0 -> 20
                    exchangeInflowChange < -20 -> 10 // Withdrawal from exchanges = HODL
                    else -> 25
                }
            components.add(
                RiskComponent(
                    "EXCHANGE INFLOWS",
                    inflowScore,
                    0.20f,
                    "Change: ${String.format(Locale.US, "+%.1f", exchangeInflowChange)}% — ${if (exchangeInflowChange > AppConstants.Risk.INFLOW_HIGH) {
                        "BEARISH (selling pressure)"
                    } else if (exchangeInflowChange < -10) {
                        "BULLISH (accumulation)"
                    } else {
                        "NEUTRAL"
                    }}",
                    exchangeInflowChange > AppConstants.Risk.INFLOW_HIGH,
                ),
            )

            // --- Open Interest Change (weight: 15%) ---
            val oiScore =
                when {
                    openInterestChange > 20 && priceChange24h < 0 -> 85 // OI up, price down = bearish
                    openInterestChange > 20 && priceChange24h > 0 -> 35 // OI up, price up = bullish
                    openInterestChange < -20 -> 30 // Delevering = healthy
                    else -> 25
                }
            components.add(
                RiskComponent(
                    "OPEN INTEREST",
                    oiScore,
                    0.15f,
                    "OI Change: ${String.format(Locale.US, "%+.1f", openInterestChange)}% — ${if (oiScore > 70) {
                        "BEARISH DIVERGENCE"
                    } else if (oiScore < 35) {
                        "HEALTHY GROWTH"
                    } else {
                        "NEUTRAL"
                    }}",
                    oiScore > 60,
                ),
            )

            // --- Macro Risk (weight: 5%) ---
            val macroScore = (macroRisk * 100).toInt()
            components.add(
                RiskComponent(
                    "MACRO",
                    macroScore,
                    0.05f,
                    "DXY/S&P Correlation Risk: ${String.format(Locale.US, "%.0f", macroRisk * 100)}%",
                    macroRisk > 0.6,
                ),
            )

            // --- Calculate weighted overall score ---
            val overall = components.sumOf { (it.score * it.weight).toDouble() }.toInt().coerceIn(0, 100)

            val level =
                when {
                    overall < 20 -> RiskLevel.VERY_LOW
                    overall < 40 -> RiskLevel.LOW
                    overall < 60 -> RiskLevel.MODERATE
                    overall < 80 -> RiskLevel.HIGH
                    else -> RiskLevel.EXTREME
                }

            // Top 3 factors by score
            val dominant =
                components
                    .sortedByDescending { it.score }
                    .take(3)
                    .map { it.name }

            val recommendation =
                when (level) {
                    RiskLevel.VERY_LOW -> "Market conditions favorable. Consider adding exposure."
                    RiskLevel.LOW -> "Low risk environment. Normal position sizing appropriate."
                    RiskLevel.MODERATE -> "Elevated risk detected. Reduce position size by 25-30%."
                    RiskLevel.HIGH -> "High risk environment. Consider taking profits or hedging."
                    RiskLevel.EXTREME -> "EXTREME RISK. Recommend reducing exposure significantly or exiting positions."
                }

            _currentScore.value = overall
            return RiskScore(overall, level, components, dominant, recommendation, System.currentTimeMillis())
        }
    }
