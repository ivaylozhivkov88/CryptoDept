package com.cryptodept.domain.usecase

import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RiskScoreEngine @Inject constructor() {

    private val _currentScore = kotlinx.coroutines.flow.MutableStateFlow<Int>(50)
    val currentScore = _currentScore.asStateFlow()

    fun observeRiskScore() = currentScore

    data class RiskScore(
        val overall: Int,                    // 0-100 (0=минимален риск, 100=максимален)
        val level: RiskLevel,
        val components: List<RiskComponent>,
        val dominantFactors: List<String>,   // Топ 3 фактора
        val recommendation: String,
        val calculatedAt: Long
    )

    data class RiskComponent(
        val name: String,
        val score: Int,          // 0-100
        val weight: Float,       // Тежест в общия score
        val signal: String,      // Описание
        val isBearish: Boolean
    )

    enum class RiskLevel(val label: String, val color: Long) {
        VERY_LOW("MINIMAL RISK",    0xFF00FF41),  // < 20
        LOW("LOW RISK",             0xFF39FF14),  // 20-40
        MODERATE("MODERATE RISK",   0xFFFFB000),  // 40-60
        HIGH("HIGH RISK",           0xFFFF6600),  // 60-80
        EXTREME("EXTREME RISK",     0xFFFF3B30)   // > 80
    }

    fun calculate(
        rsi: Double,
        fundingRate: Double,         // % (напр. 0.08 = 0.08%)
        longShortRatio: Double,      // > 1.0 = повече лонгове
        fearGreedIndex: Int,         // 0-100
        exchangeInflowChange: Double, // % промяна (положително = повече влизат в борсата)
        openInterestChange: Double,  // % промяна за 24h
        priceChange24h: Double,      // % промяна на цената
        macroRisk: Double = 0.5      // 0.0-1.0 (от DXY + S&P корелация)
    ): RiskScore {

        val components = mutableListOf<RiskComponent>()

        // --- RSI компонент (тежест: 15%) ---
        val rsiScore = when {
            rsi > 80 -> 90
            rsi > 70 -> 70
            rsi > 60 -> 40
            rsi in 40.0..60.0 -> 20
            rsi < 30 -> 15  // Oversold = нисък риск за нов шорт
            else -> 30
        }
        components.add(RiskComponent("RSI", rsiScore, 0.15f,
            "RSI: ${String.format("%.1f", rsi)} — ${if (rsi > 70) "OVERBOUGHT" else if (rsi < 30) "OVERSOLD" else "NEUTRAL"}",
            rsi > 70))

        // --- Funding Rate компонент (тежест: 20%) ---
        val fundingScore = when {
            fundingRate > 0.10 -> 95   // Extreme — crash risk
            fundingRate > 0.05 -> 75   // High
            fundingRate > 0.02 -> 40   // Elevated
            fundingRate in -0.02..0.02 -> 20  // Normal
            fundingRate < -0.05 -> 10  // Negative = bears paying = low crash risk
            else -> 30
        }
        val fundingPct = String.format("%.4f", fundingRate)
        components.add(RiskComponent("FUNDING RATE", fundingScore, 0.20f,
            "Rate: $fundingPct% — ${when { fundingRate > 0.05 -> "ELEVATED (longs overlevered)" ; fundingRate < -0.02 -> "NEGATIVE (shorts overlevered)" ; else -> "NORMAL" }}",
            fundingRate > 0.05))

        // --- Long/Short Ratio (тежест: 15%) ---
        val lsScore = when {
            longShortRatio > 3.0 -> 85  // Extreme longs
            longShortRatio > 2.0 -> 65
            longShortRatio > 1.5 -> 45
            longShortRatio in 0.8..1.5 -> 20
            longShortRatio < 0.5 -> 15  // Extreme shorts = contrarian bullish
            else -> 30
        }
        components.add(RiskComponent("LONG/SHORT", lsScore, 0.15f,
            "Ratio: ${String.format("%.2f", longShortRatio)} — ${if (longShortRatio > 2.0) "CROWDED LONGS" else if (longShortRatio < 0.7) "CROWDED SHORTS" else "BALANCED"}",
            longShortRatio > 2.0))

        // --- Fear & Greed (тежест: 10%) ---
        val fgScore = when {
            fearGreedIndex > 85 -> 90  // Extreme Greed
            fearGreedIndex > 70 -> 65
            fearGreedIndex in 45..70 -> 30
            fearGreedIndex < 25 -> 10  // Extreme Fear = buy signal
            else -> 25
        }
        components.add(RiskComponent("FEAR & GREED", fgScore, 0.10f,
            "Index: $fearGreedIndex — ${when { fearGreedIndex > 75 -> "EXTREME GREED" ; fearGreedIndex > 55 -> "GREED" ; fearGreedIndex < 25 -> "EXTREME FEAR" ; fearGreedIndex < 45 -> "FEAR" ; else -> "NEUTRAL" }}",
            fearGreedIndex > 70))

        // --- Exchange Inflows (тежест: 20%) ---
        val inflowScore = when {
            exchangeInflowChange > 50 -> 90  // Масово изпращане към борсите
            exchangeInflowChange > 20 -> 70
            exchangeInflowChange > 5  -> 40
            exchangeInflowChange in -5.0..5.0 -> 20
            exchangeInflowChange < -20 -> 10  // Теглене от борсите = HODL
            else -> 25
        }
        components.add(RiskComponent("EXCHANGE INFLOWS", inflowScore, 0.20f,
            "Change: ${String.format("+%.1f", exchangeInflowChange)}% — ${if (exchangeInflowChange > 20) "BEARISH (selling pressure)" else if (exchangeInflowChange < -10) "BULLISH (accumulation)" else "NEUTRAL"}",
            exchangeInflowChange > 20))

        // --- Open Interest Change (тежест: 15%) ---
        val oiScore = when {
            openInterestChange > 20 && priceChange24h < 0 -> 85  // OI расте, цена пада = bearish
            openInterestChange > 20 && priceChange24h > 0 -> 35  // OI расте, цена расте = bullish
            openInterestChange < -20 -> 30  // Delevering = healthy
            else -> 25
        }
        components.add(RiskComponent("OPEN INTEREST", oiScore, 0.15f,
            "OI Change: ${String.format(java.util.Locale.US, "%+.1f", openInterestChange)}% — ${if (oiScore > 70) "BEARISH DIVERGENCE" else if (oiScore < 35) "HEALTHY GROWTH" else "NEUTRAL"}",
            oiScore > 60))

        // --- Macro Risk (тежест: 5%) ---
        val macroScore = (macroRisk * 100).toInt()
        components.add(RiskComponent("MACRO", macroScore, 0.05f,
            "DXY/S&P Correlation Risk: ${String.format("%.0f", macroRisk * 100)}%",
            macroRisk > 0.6))

        // --- Изчисли weighted overall score ---
        val overall = components.sumOf { (it.score * it.weight).toDouble() }.toInt().coerceIn(0, 100)

        val level = when {
            overall < 20 -> RiskLevel.VERY_LOW
            overall < 40 -> RiskLevel.LOW
            overall < 60 -> RiskLevel.MODERATE
            overall < 80 -> RiskLevel.HIGH
            else -> RiskLevel.EXTREME
        }

        // Топ 3 фактора по score
        val dominant = components.sortedByDescending { it.score }
            .take(3)
            .map { it.name }

        val recommendation = when (level) {
            RiskLevel.VERY_LOW   -> "Market conditions favorable. Consider adding exposure."
            RiskLevel.LOW        -> "Low risk environment. Normal position sizing appropriate."
            RiskLevel.MODERATE   -> "Elevated risk detected. Reduce position size by 25-30%."
            RiskLevel.HIGH       -> "High risk environment. Consider taking profits or hedging."
            RiskLevel.EXTREME    -> "EXTREME RISK. Recommend reducing exposure significantly or exiting positions."
        }

        _currentScore.value = overall
        return RiskScore(overall, level, components, dominant, recommendation, System.currentTimeMillis())
    }
}

