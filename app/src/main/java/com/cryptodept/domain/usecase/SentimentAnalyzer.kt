package com.cryptodept.domain.usecase

import com.cryptodept.data.api.RssNewsParser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SentimentAnalyzer @Inject constructor(
    private val rssParser: RssNewsParser
) {
    private val BULLISH_WORDS = setOf(
        "bull", "bullish", "surge", "pump", "moon", "ath", "breakout",
        "rally", "gains", "adoption", "accumulate", "buy", "long", "strong", "growth", "upgrade", "positive"
    )
    private val BEARISH_WORDS = setOf(
        "bear", "bearish", "crash", "dump", "drop", "fall", "short",
        "sell", "weak", "correction", "liquidation", "fud", "ban", "hack", "scam", "negative", "fear"
    )

    suspend fun analyzeCoin(symbol: String): SentimentResult {
        val headlines = mutableListOf<String>()

        // Source 1: Reddit r/CryptoCurrency
        try {
            val redditUrl = "https://www.reddit.com/r/CryptoCurrency/search.rss?q=$symbol&sort=new&restrict_sr=on"
            val redditItems = rssParser.parseUrl(redditUrl).take(15)
            headlines.addAll(redditItems.map { it.title })
        } catch (e: Exception) {
            // Log or ignore
        }

        // Source 2: CryptoPanic RSS
        try {
            val panicUrl = "https://cryptopanic.com/news/rss/"
            val panicItems = rssParser.parseUrl(panicUrl).take(20)
            // Filter items that mention the symbol
            headlines.addAll(panicItems.filter {
                it.title.contains(symbol, ignoreCase = true) || it.description.contains(symbol, ignoreCase = true)
            }.map { it.title })
        } catch (e: Exception) {
            // Log or ignore
        }

        var bullish = 0
        var bearish = 0
        var neutral = 0

        if (headlines.isEmpty()) {
            return SentimentResult(symbol, 0, 0, 100, 0, SentimentVerdict.NEUTRAL)
        }

        headlines.forEach { text ->
            val lowerText = text.lowercase()
            val bullScore = BULLISH_WORDS.count { lowerText.contains(it) }
            val bearScore = BEARISH_WORDS.count { lowerText.contains(it) }
            when {
                bullScore > bearScore -> bullish++
                bearScore > bullScore -> bearish++
                else -> neutral++
            }
        }

        val total = (bullish + bearish + neutral).coerceAtLeast(1)
        return SentimentResult(
            symbol = symbol,
            bullishPercent = (bullish * 100) / total,
            bearishPercent = (bearish * 100) / total,
            neutralPercent = (neutral * 100) / total,
            totalAnalyzed = total,
            verdict = when {
                bullish > bearish * 1.5 -> SentimentVerdict.STRONGLY_BULLISH
                bullish > bearish -> SentimentVerdict.BULLISH
                bearish > bullish * 1.5 -> SentimentVerdict.STRONGLY_BEARISH
                bearish > bullish -> SentimentVerdict.BEARISH
                else -> SentimentVerdict.NEUTRAL
            }
        )
    }

    fun calculatePulse(symbol: String): Int {
        // Social pulse: 0-100 scale
        // 0 = extreme bearish, 50 = neutral, 100 = extreme bullish
        // Default to 50 (neutral) - can be enhanced with real-time data
        return 50
    }

    fun getPulseLabel(pulse: Int): String {
        return when {
            pulse >= 80 -> "EUPHORIA"
            pulse >= 65 -> "BULLISH"
            pulse >= 55 -> "SLIGHTLY BULLISH"
            pulse >= 45 -> "NEUTRAL"
            pulse >= 35 -> "SLIGHTLY BEARISH"
            pulse >= 20 -> "BEARISH"
            else -> "FEAR"
        }
    }
}

data class SentimentResult(
    val symbol: String,
    val bullishPercent: Int,
    val bearishPercent: Int,
    val neutralPercent: Int,
    val totalAnalyzed: Int,
    val verdict: SentimentVerdict
)

enum class SentimentVerdict { STRONGLY_BULLISH, BULLISH, NEUTRAL, BEARISH, STRONGLY_BEARISH }