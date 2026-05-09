package com.cryptodept.domain.usecase

import android.util.Log
import com.cryptodept.data.api.RssNewsParser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SentimentAnalyzer
    @Inject
    constructor(
        private val rssParser: RssNewsParser,
    ) {
        // Score-based word mapping
        private val BULLISH_WORDS =
            mapOf(
                "bull" to 2,
                "bullish" to 3,
                "surge" to 3,
                "pump" to 2,
                "moon" to 4,
                "ath" to 4,
                "breakout" to 3,
                "rally" to 3,
                "gains" to 2,
                "adoption" to 3,
                "accumulate" to 2,
                "buy" to 1,
                "long" to 1,
                "strong" to 2,
                "growth" to 2,
                "upgrade" to 2,
                "positive" to 1,
                "institutional" to 3,
                "inflow" to 3,
                "support" to 2,
                "reversal" to 2,
                "bottom" to 3,
                "explosion" to 4,
                "soaring" to 3,
                "undervalued" to 3,
                "accumulating" to 2,
                "listing" to 2,
                "partnership" to 3,
                "massive" to 2,
                "holding" to 1,
            )

        private val BEARISH_WORDS =
            mapOf(
                "bear" to 2,
                "bearish" to 3,
                "crash" to 4,
                "dump" to 3,
                "drop" to 2,
                "fall" to 2,
                "short" to 1,
                "sell" to 1,
                "weak" to 2,
                "correction" to 2,
                "liquidation" to 3,
                "fud" to 4,
                "ban" to 4,
                "hack" to 5,
                "scam" to 5,
                "negative" to 2,
                "fear" to 3,
                "outflow" to 3,
                "resistance" to 2,
                "death" to 4,
                "collapse" to 5,
                "plunge" to 4,
                "disaster" to 5,
                "rug" to 5,
                "caution" to 2,
                "oversold" to 1,
                "unstable" to 3,
                "rejected" to 2,
                "panic" to 4,
                "bloodbath" to 5,
            )

        // Simple in-memory cache: Map<symbol, SentimentResult>
        private val sentimentCache = mutableMapOf<String, SentimentResult>()
        private val cacheTimestamps = mutableMapOf<String, Long>()
        private val CACHE_DURATION_MS = 10 * 60 * 1000L // 10 minutes

        suspend fun analyzeCoin(symbol: String): SentimentResult {
            // Check cache first
            val now = System.currentTimeMillis()
            val cachedResult = sentimentCache[symbol]
            val cachedTime = cacheTimestamps[symbol] ?: 0L

            if (cachedResult != null && (now - cachedTime) < CACHE_DURATION_MS) {
                Log.d("SentimentAnalyzer", "Cache hit for $symbol")
                return cachedResult
            }

            val headlines = mutableListOf<String>()

            // Source 1: CryptoPanic RSS + Others
            try {
                val allItems = rssParser.fetchAllSources()
                headlines.addAll(
                    allItems
                        .filter { it.title.contains(symbol, ignoreCase = true) || it.description.contains(symbol, ignoreCase = true) }
                        .take(40)
                        .map { it.title },
                )
            } catch (e: Exception) {
                Log.e("SentimentAnalyzer", "RSS fetch failed for $symbol: ${e.message}")
            }

            // Source 2: Reddit r/CryptoCurrency (secondary)
            try {
                val redditUrl = "https://www.reddit.com/r/CryptoCurrency/search.rss?q=$symbol&sort=new&restrict_sr=on"
                val redditItems = rssParser.parseUrl(redditUrl).take(20)
                headlines.addAll(redditItems.map { it.title })
            } catch (e: Exception) {
                Log.d("SentimentAnalyzer", "Reddit RSS skipped: ${e.message}")
            }

            var totalBullScore = 0
            var totalBearScore = 0
            var neutralCount = 0

            // If we have no data, return neutral
            if (headlines.isEmpty()) {
                val defaultResult = SentimentResult(symbol, 0, 0, 100, 0, SentimentVerdict.NEUTRAL)
                sentimentCache[symbol] = defaultResult
                cacheTimestamps[symbol] = now
                return defaultResult
            }

            // Analyze each headline with weighted scoring
            headlines.forEach { text ->
                val lowerText = text.lowercase()
                var currentBull = 0
                var currentBear = 0

                BULLISH_WORDS.forEach { (word, score) ->
                    if (lowerText.contains(word)) currentBull += score
                }
                BEARISH_WORDS.forEach { (word, score) ->
                    if (lowerText.contains(word)) currentBear += score
                }

                totalBullScore += currentBull
                totalBearScore += currentBear
                if (currentBull == 0 && currentBear == 0) neutralCount++
            }

            val totalScore = (totalBullScore + totalBearScore + neutralCount).coerceAtLeast(1)

            val bullishPercent = (totalBullScore * 100) / totalScore
            val bearishPercent = (totalBearScore * 100) / totalScore
            val neutralPercent = 100 - bullishPercent - bearishPercent

            val result =
                SentimentResult(
                    symbol = symbol,
                    bullishPercent = bullishPercent,
                    bearishPercent = bearishPercent,
                    neutralPercent = neutralPercent,
                    totalAnalyzed = headlines.size,
                    verdict =
                        when {
                            totalBullScore > totalBearScore * 2.0 -> SentimentVerdict.STRONGLY_BULLISH
                            totalBullScore > totalBearScore * 1.2 -> SentimentVerdict.BULLISH
                            totalBearScore > totalBullScore * 2.0 -> SentimentVerdict.STRONGLY_BEARISH
                            totalBearScore > totalBullScore * 1.2 -> SentimentVerdict.BEARISH
                            else -> SentimentVerdict.NEUTRAL
                        },
                )

            // Cache the result
            sentimentCache[symbol] = result
            cacheTimestamps[symbol] = now
            return result
        }

        suspend fun calculatePulse(symbol: String): Int {
            val result = analyzeCoin(symbol)
            return calculatePulse(result)
        }

        fun calculatePulse(sentiment: SentimentResult?): Int {
            if (sentiment == null) return 50

            // Scale sentiment to pulse (0-100)
            // 50 = neutral, higher = bullish, lower = bearish
            val bullishPart = sentiment.bullishPercent * 1.5f
            val bearishPart = sentiment.bearishPercent * 1.5f

            return (50f + bullishPart - bearishPart).toInt().coerceIn(0, 100)
        }

        fun getPulseLabel(pulse: Int): String =
            when {
                pulse >= 85 -> "EUPHORIA"
                pulse >= 70 -> "BULLISH"
                pulse >= 55 -> "OPTIMISTIC"
                pulse >= 45 -> "NEUTRAL"
                pulse >= 30 -> "CAUTIOUS"
                pulse >= 15 -> "BEARISH"
                else -> "EXTREME_FEAR"
            }

        fun getSentimentEmoji(verdict: SentimentVerdict): String =
            when (verdict) {
                SentimentVerdict.STRONGLY_BULLISH -> "🚀"
                SentimentVerdict.BULLISH -> "📈"
                SentimentVerdict.NEUTRAL -> "💀"
                SentimentVerdict.BEARISH -> "📉"
                SentimentVerdict.STRONGLY_BEARISH -> "🔻"
            }

        fun clearCache() {
            sentimentCache.clear()
            cacheTimestamps.clear()
        }
    }

data class SentimentResult(
    val symbol: String,
    val bullishPercent: Int,
    val bearishPercent: Int,
    val neutralPercent: Int,
    val totalAnalyzed: Int,
    val verdict: SentimentVerdict,
)

enum class SentimentVerdict {
    STRONGLY_BULLISH,
    BULLISH,
    NEUTRAL,
    BEARISH,
    STRONGLY_BEARISH,
}
