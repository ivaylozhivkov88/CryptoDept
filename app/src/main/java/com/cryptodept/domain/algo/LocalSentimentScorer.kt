package com.cryptodept.domain.algo

import com.cryptodept.domain.model.NewsSentiment
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalSentimentScorer
    @Inject
    constructor() {
        private val bullishWords =
            setOf(
                "surge",
                "bullish",
                "moon",
                "pump",
                "gain",
                "breakout",
                "accumulate",
                "rally",
                "adoption",
                "partnership",
                "integrated",
                "launch",
                "upgrade",
                "ath",
                "profit",
                "positive",
                "growth",
                "support",
                "hodl",
                "green",
            )

        private val bearishWords =
            setOf(
                "crash",
                "bearish",
                "dump",
                "drop",
                "hack",
                "scam",
                "fraud",
                "regulation",
                "ban",
                "liquidated",
                "resistance",
                "red",
                "selloff",
                "fud",
                "warning",
                "correction",
                "plunge",
                "downside",
                "negative",
                "outflow",
                "bankrupt",
            )

        fun analyze(text: String): NewsSentiment {
            val words = text.lowercase().split(Regex("\\W+"))
            var score = 0

            words.forEach { word ->
                if (bullishWords.contains(word)) score++
                if (bearishWords.contains(word)) score--
            }

            return when {
                score > 0 -> NewsSentiment.BULLISH
                score < 0 -> NewsSentiment.BEARISH
                else -> NewsSentiment.NEUTRAL
            }
        }

        fun getScore(text: String): Int {
            val words = text.lowercase().split(Regex("\\W+"))
            var score = 0
            words.forEach { word ->
                if (bullishWords.contains(word)) score += 10
                if (bearishWords.contains(word)) score -= 10
            }
            return (50 + score).coerceIn(0, 100)
        }
    }
