package com.cryptodept.data.repository

import com.cryptodept.data.api.*
import com.cryptodept.data.db.NewsDao
import com.cryptodept.domain.algo.LocalSentimentScorer
import com.cryptodept.domain.model.NewsSentiment
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class NewsRepositoryTest {
    private val newsApi: NewsApiService = mockk()
    private val coinGeckoApi: CoinGeckoApi = mockk()
    private val rssParser: RssNewsParser = mockk()
    private val redditClient: RedditClient = mockk()
    private val sentimentScorer: LocalSentimentScorer = mockk(relaxed = true)
    private val newsDao: NewsDao = mockk(relaxed = true)

    private lateinit var repository: NewsRepositoryImpl

    @Before
    fun setup() {
        repository = NewsRepositoryImpl(
            newsApi,
            coinGeckoApi,
            rssParser,
            redditClient,
            sentimentScorer,
            newsDao
        )
    }

    @Test
    fun `refreshNews fetches from fallback sources when API key is missing`() = runTest {
        // Mocking behavior for fallback when BuildConfig.CRYPTOPANIC_API_KEY is blank
        // Note: In tests, BuildConfig fields might be different. 
        // NewsRepositoryImpl checks if it's blank.
        
        coEvery { coinGeckoApi.getNews() } returns CoinGeckoNewsResponse(emptyList())
        coEvery { rssParser.fetchAllSources() } returns listOf(
            RssNewsParser.RssItem(
                title = "BTC breaking news",
                link = "link",
                pubDate = "Tue, 05 May 2026 10:00:00 +0000",
                description = "desc",
                source = "Source"
            )
        )
        coEvery { redditClient.fetchCryptoReddit() } returns emptyList()
        every { sentimentScorer.analyze(any()) } returns NewsSentiment.BULLISH

        val result = repository.refreshNews()

        assertThat(result.isSuccess).isTrue()
        coVerify { newsDao.insertNews(any()) }
        coVerify { rssParser.fetchAllSources() }
    }

    private fun assertThat(actual: Boolean) = com.google.common.truth.Truth.assertThat(actual)
}
