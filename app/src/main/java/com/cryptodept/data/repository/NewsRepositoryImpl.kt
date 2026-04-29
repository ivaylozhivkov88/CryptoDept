package com.cryptodept.data.repository

import com.cryptodept.domain.model.NewsItem
import com.cryptodept.domain.model.RssNewsItem
import com.cryptodept.domain.model.NewsSentiment
import com.cryptodept.domain.repository.NewsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepositoryImpl @Inject constructor() : NewsRepository {

    override fun getCryptoPanicNews(): Flow<List<NewsItem>> = flow {
        val mockNews = listOf(
            NewsItem(
                title = "Bitcoin Market Analysis",
                source = "CryptoPanic",
                url = "https://example.com",
                publishedAt = "2024-05-22T10:00:00Z"
            )
        )
        emit(mockNews)
    }

    override suspend fun getRssNews(forceRefresh: Boolean): Result<List<RssNewsItem>> {
        return try {
            val mockRss = listOf(
                RssNewsItem(
                    id = "rss_1",
                    title = "Ethereum Spot ETF Inflows Surpass Expectations",
                    url = "https://example.com/rss1",
                    publishedAt = System.currentTimeMillis(),
                    source = "CoinTelegraph",
                    category = "Institutional",
                    sentiment = NewsSentiment.BULLISH,
                    relevantCoins = listOf("ETH"),
                    isBreaking = true
                )
            )
            Result.success(mockRss)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeRssNews(): Flow<List<RssNewsItem>> = flow {
        getRssNews().onSuccess { emit(it) }
    }

    override fun getAllNews(): Flow<List<RssNewsItem>> = flow {
        emit(emptyList())
    }
}
