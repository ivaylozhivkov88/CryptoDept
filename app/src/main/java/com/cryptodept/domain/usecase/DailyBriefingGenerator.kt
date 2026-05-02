package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.CalendarEvent
import com.cryptodept.domain.model.LiquidationLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyBriefingGenerator @Inject constructor() {
    // ... existing data classes ...

    /**
     * Форматира число като милиони със суфикс "M" (е.g., 15.2M)
     */
    private fun formatMillions(value: Double): String {
        return String.format(Locale.ENGLISH, "%.1f", value) + "M"
    }

    /**
     * Форматира цена със хилядиделител (е.g., 43,250.50)
     */
    private fun formatPrice(value: Double): String {
        return String.format(Locale.ENGLISH, "%,.0f", value)
    }
    data class DailyBriefing(
        val date: String,
        val generatedAt: Long,
        val marketSentence: String,       // 1 изречение обобщение
        val riskScore: RiskScoreEngine.RiskScore,
        val keyMetrics: List<BriefingMetric>,
        val topAlerts: List<BriefingAlert>,
        val tradingSuggestion: String,
        val watchLevels: List<WatchLevel>
    )

    data class BriefingMetric(
        val label: String,
        val value: String,
        val change: String?,
        val sentiment: String  // "BULLISH", "BEARISH", "NEUTRAL"
    )

    data class BriefingAlert(
        val severity: AlertSeverity,
        val title: String,
        val detail: String
    )

    data class WatchLevel(
        val coin: String,
        val price: Double,
        val type: String,   // "SUPPORT", "RESISTANCE", "LIQUIDATION_CLUSTER"
        val significance: String
    )

    enum class AlertSeverity { INFO, WARNING, CRITICAL }

    fun generate(
        btcPrice: Double,
        btcChange24h: Double,
        riskScore: RiskScoreEngine.RiskScore,
        fundingRate: Double,
        fearGreedIndex: Int,
        exchangeInflowChange: Double,
        upcomingEvents: List<CalendarEvent>,
        topLiquidationLevel: LiquidationLevel?
    ): DailyBriefing {

        val date = SimpleDateFormat("EEEE, MMMM d yyyy", Locale.ENGLISH)
            .format(Date())

        // --- Market Sentence ---
        val marketSentence = buildMarketSentence(btcPrice, btcChange24h, riskScore, fearGreedIndex)

        // --- Key Metrics ---
        val keyMetrics = listOf(
            BriefingMetric("BTC PRICE",
                "$$${formatPrice(btcPrice)}",
                "${if (btcChange24h > 0) "+" else ""}${String.format(Locale.ENGLISH, "%.2f", btcChange24h)}%",
                if (btcChange24h > 0) "BULLISH" else "BEARISH"),
            BriefingMetric("FUNDING RATE",
                "${String.format(Locale.ENGLISH, "%.4f", fundingRate)}%",
                null,
                when { fundingRate > 0.05 -> "BEARISH" ; fundingRate < -0.02 -> "BULLISH" ; else -> "NEUTRAL" }),
            BriefingMetric("FEAR & GREED",
                "$fearGreedIndex",
                null,
                when { fearGreedIndex > 70 -> "BEARISH" ; fearGreedIndex < 30 -> "BULLISH" ; else -> "NEUTRAL" }),
            BriefingMetric("EXCHANGE INFLOWS",
                "${if (exchangeInflowChange > 0) "↑" else "↓"} ${String.format(Locale.ENGLISH, "%.1f", Math.abs(exchangeInflowChange))}%",
                null,
                if (exchangeInflowChange > 15) "BEARISH" else if (exchangeInflowChange < -15) "BULLISH" else "NEUTRAL"),
            BriefingMetric("RISK SCORE",
                "${riskScore.overall}/100",
                null,
                riskScore.level.label)
        )

        // --- Alerts ---
        val alerts = mutableListOf<BriefingAlert>()

        if (fundingRate > 0.10) alerts.add(BriefingAlert(AlertSeverity.CRITICAL,
            "EXTREME FUNDING RATE",
            "Funding at ${String.format(Locale.ENGLISH, "%.4f", fundingRate)}% — Market severely overlevered. High crash risk."))

        if (fearGreedIndex > 85) alerts.add(BriefingAlert(AlertSeverity.WARNING,
            "EXTREME GREED DETECTED",
            "Fear & Greed at $fearGreedIndex. Historically precedes corrections."))

        if (exchangeInflowChange > 30) alerts.add(BriefingAlert(AlertSeverity.WARNING,
            "EXCHANGE INFLOW SPIKE",
            "Exchange inflows up ${String.format(Locale.ENGLISH, "%.0f", exchangeInflowChange)}%. Potential selling pressure incoming."))

        upcomingEvents.filter { it.daysUntil <= 3 }.take(2).forEach { event ->
            alerts.add(BriefingAlert(AlertSeverity.INFO,
                "UPCOMING EVENT: ${event.coins.joinToString(", ")}",
                "${event.title} — In ${event.daysUntil} day(s)"))
        }

        topLiquidationLevel?.let {
            if (it.isSignificant) {
                val totalM = (it.longLiquidationUsd + it.shortLiquidationUsd) / 1_000_000
                alerts.add(BriefingAlert(AlertSeverity.INFO,
                    "LIQUIDATION CLUSTER",
                    "$$${formatPrice(it.price)} — ${formatMillions(totalM)} in liquidations at this level"))
            }
        }

        return DailyBriefing(
            date = date,
            generatedAt = System.currentTimeMillis(),
            marketSentence = marketSentence,
            riskScore = riskScore,
            keyMetrics = keyMetrics,
            topAlerts = alerts.sortedByDescending { it.severity.ordinal },
            tradingSuggestion = riskScore.recommendation,
            watchLevels = emptyList()
        )
    }

    private fun buildMarketSentence(
        price: Double, change: Double,
        risk: RiskScoreEngine.RiskScore, fg: Int
    ): String {
        val priceStr = "$$${formatPrice(price)}"
        val changeStr = "${if (change > 0) "+" else ""}${String.format(Locale.ENGLISH, "%.2f", change)}%"
        val sentiment = when {
            risk.overall > 70 -> "showing elevated risk"
            risk.overall > 50 -> "in uncertain territory"
            risk.overall < 30 -> "in a favorable environment"
            else -> "trading in a balanced market"
        }
        val fgStr = when {
            fg > 75 -> "extreme greed"
            fg > 55 -> "greed"
            fg < 25 -> "extreme fear"
            fg < 45 -> "fear"
            else -> "neutral sentiment"
        }
        return "BTC trading at $priceStr ($changeStr 24h), $sentiment with $fgStr (F&G: $fg). Risk Score: ${risk.overall}/100."
    }
}