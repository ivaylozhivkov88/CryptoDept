package com.cryptodept.domain.usecase

import com.cryptodept.domain.algo.TreemapItem
import com.cryptodept.domain.model.AudienceProfile
import com.cryptodept.domain.model.NetworkHealth
import java.util.Locale
import javax.inject.Inject

class DailyRecapPromptBuilder
    @Inject
    constructor() {
        fun build(
            audience: AudienceProfile,
            health: NetworkHealth,
            topMovers: List<TreemapItem>,
            recentNews: List<String>,
        ): String {
            val topMoversStr = topMovers.take(5).joinToString { "${it.symbol} (${String.format(Locale.US, "%.1f", it.change24h)}%)" }
            val newsStr = if (recentNews.isEmpty()) "No significant news." else recentNews.take(3).joinToString("; ")

            return """
                Generate a Daily Market Recap for a ${audience.label} audience.
                TONE: ${audience.tone}
                TECHNICAL LEVEL: ${audience.technicalLevel}
                OUTPUT LANGUAGE: English (EN)
                
                STRICT STRUCTURE (Required):
                1. HOOK: 15-20 words, attention-grabbing, immediate value.
                2. CONTEXT: 2-3 sentences summarizing current market sentiment and trends.
                3. NUMBERS: 3-5 key metrics (Price action, Fear & Greed, Hashrate/Gas).
                4. WHAT IT MEANS: 3-4 sentences of deep analysis (not just "price went up").
                5. WHAT TO WATCH: 2-3 specific bullet points for the next 24-48 hours.
                6. CTA: Engaging call to action relevant to ${audience.label}.
                
                DATA FOR ANALYSIS:
                - Fear & Greed Index: ${health.fearGreedIndex} (${health.fearGreedLabel})
                - Network Status: BTC Hashrate: ${health.btcHashrate}, ETH Gas: ${health.ethGas}
                - Social Pulse: ${health.socialPulse} (${health.socialPulseLabel})
                - Top Movers: $topMoversStr
                - Recent Market Events: $newsStr
                
                CRITICAL RULES:
                - DO NOT use crypto clichés (e.g., "to the moon", "lambos", "DYOR", "NFA").
                - Respond ONLY in English.
                - Stay data-driven and professional.
                - Length constraint: ${audience.preferredLength}.
                """.trimIndent()
        }
    }
