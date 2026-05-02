package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.NewsSentiment
import com.cryptodept.domain.repository.NewsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SentimentAnalyzer @Inject constructor(
    private val newsRepository: NewsRepository
) {
    /**
     * Calculates the "Social Pulse" based on recent news sentiment and volume.
     * Scale: 0 (Extreme Panic) to 100 (Extreme Euphoria)
     */
    suspend fun calculatePulse(symbol: String): Int {
        val news = newsRepository.getNews(symbol).first()
        if (news.isEmpty()) return 50 // Neutral fallback

        val recentNews = news.take(20)
        var score = 50f
        
        recentNews.forEach { item ->
            when (item.sentiment) {
                NewsSentiment.BULLISH -> score += 5f
                NewsSentiment.BEARISH -> score -= 5f
                NewsSentiment.NEUTRAL -> {}
            }
        }
        
        return score.toInt().coerceIn(0, 100)
    }
    
    fun getPulseLabel(score: Int): String {
        return when {
            score >= 80 -> "EXTREME_EUPHORIA"
            score >= 60 -> "BULLISH_PULSE"
            score >= 40 -> "NEUTRAL_SIGNAL"
            score >= 20 -> "BEARISH_PULSE"
            else -> "PANIC_DETECTED"
        }
    }
}
