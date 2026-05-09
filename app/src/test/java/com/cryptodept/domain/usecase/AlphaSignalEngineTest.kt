package com.cryptodept.domain.usecase

import com.cryptodept.domain.algo.LocalSentimentScorer
import com.cryptodept.domain.model.Blockchain
import com.cryptodept.domain.model.NewsItem
import com.cryptodept.domain.model.NewsSentiment
import com.cryptodept.domain.model.WhaleTransaction
import com.cryptodept.domain.repository.NewsRepository
import com.cryptodept.domain.repository.WhaleRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class AlphaSignalEngineTest {
    private val whaleRepository: WhaleRepository = mockk()
    private val newsRepository: NewsRepository = mockk()
    private val sentimentScorer: LocalSentimentScorer = mockk()
    private lateinit var engine: AlphaSignalEngine

    @Before
    fun setup() {
        engine = AlphaSignalEngine(whaleRepository, newsRepository, sentimentScorer)
    }

    @Test
    fun `signals emits ALPHA_CONFLUENCE when both volume and sentiment are high`() = runTest {
        // Mock Whales
        val whaleTx = WhaleTransaction(
            id = "1",
            blockchain = Blockchain.BITCOIN,
            amount = 100.0,
            amountUsd = 6_000_000.0,
            symbol = "BTC",
            fromAddress = "A",
            toAddress = "B",
            timestamp = System.currentTimeMillis(),
            transactionHash = "0xABC"
        )
        every { whaleRepository.getWhaleTransactions() } returns flowOf(listOf(whaleTx))

        // Mock News
        val news = NewsItem(
            id = "1",
            title = "BTC SURGE INBOUND",
            url = "",
            source = "",
            publishedAt = System.currentTimeMillis(),
            sentiment = NewsSentiment.BULLISH,
            currencies = persistentListOf("BTC")
        )
        every { newsRepository.getNews() } returns flowOf(listOf(news))
        every { sentimentScorer.getScore(any()) } returns 80

        val signals = engine.signals.first()
        
        assertThat(signals).isNotEmpty()
        assertThat(signals.first().type).isEqualTo(SignalType.ALPHA_CONFLUENCE)
        assertThat(signals.first().strength).isAtLeast(80)
    }

    @Test
    fun `signals emits BULLISH_WHALE_ACCUMULATION when volume high but sentiment neutral`() = runTest {
        val whaleTx = WhaleTransaction(
            id = "1",
            blockchain = Blockchain.BITCOIN,
            amount = 100.0,
            amountUsd = 6_000_000.0,
            symbol = "BTC",
            fromAddress = "A",
            toAddress = "B",
            timestamp = System.currentTimeMillis(),
            transactionHash = "0xABC"
        )
        every { whaleRepository.getWhaleTransactions() } returns flowOf(listOf(whaleTx))

        every { newsRepository.getNews() } returns flowOf(emptyList())
        // Default sentiment will be 50.0

        val signals = engine.signals.first()
        
        assertThat(signals).isNotEmpty()
        assertThat(signals.first().type).isEqualTo(SignalType.BULLISH_WHALE_ACCUMULATION)
    }
}
