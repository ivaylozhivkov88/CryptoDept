package com.cryptodept.domain.usecase

import com.cryptodept.domain.algo.LocalSentimentScorer
import com.cryptodept.domain.model.WhaleTransaction
import com.cryptodept.domain.repository.NewsRepository
import com.cryptodept.domain.repository.WhaleRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

data class AlphaSignal(
    val coin: String,
    val type: SignalType,
    val strength: Int, // 0-100
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class SignalType {
    BULLISH_WHALE_ACCUMULATION,
    BEARISH_WHALE_DISTRIBUTION,
    SENTIMENT_PUMP,
    SENTIMENT_DUMP,
    ALPHA_CONFLUENCE
}

@Singleton
class AlphaSignalEngine @Inject constructor(
    private val whaleRepository: WhaleRepository,
    private val newsRepository: NewsRepository,
    private val sentimentScorer: LocalSentimentScorer
) {
    private val WHALE_THRESHOLD_USD = 1_000_000.0
    private val SENTIMENT_THRESHOLD = 70

    val signals: Flow<List<AlphaSignal>> = combine(
        whaleRepository.getWhaleTransactions(),
        newsRepository.getNews()
    ) { whales, news ->
        val newSignals = mutableListOf<AlphaSignal>()
        
        // 1. Analyze Whale activity
        val btcWhales = whales.filter { it.symbol == "BTC" && it.timestamp > System.currentTimeMillis() - 3600000 }
        val btcWhaleVolume = btcWhales.sumOf { it.amountUsd }
        
        // 2. Analyze Sentiment
        val btcNews = news.filter { it.currencies.contains("BTC") }
        val avgSentiment = if (btcNews.isNotEmpty()) {
            btcNews.map { sentimentScorer.getScore(it.title) }.average()
        } else 50.0

        // 3. Detect Confluence
        if (btcWhaleVolume > WHALE_THRESHOLD_USD && avgSentiment > SENTIMENT_THRESHOLD) {
            newSignals.add(AlphaSignal(
                coin = "BTC",
                type = SignalType.ALPHA_CONFLUENCE,
                strength = 85,
                reason = "Strong Whale accumulation ($${String.format(java.util.Locale.US, "%.1fM", btcWhaleVolume/1_000_000)}) aligned with Bullish sentiment (${avgSentiment.toInt()}/100)"
            ))
        } else if (btcWhaleVolume > WHALE_THRESHOLD_USD) {
            newSignals.add(AlphaSignal(
                coin = "BTC",
                type = SignalType.BULLISH_WHALE_ACCUMULATION,
                strength = 70,
                reason = "Large Whale moves detected ($${String.format(java.util.Locale.US, "%.1fM", btcWhaleVolume/1_000_000)})"
            ))
        }

        newSignals
    }
}
