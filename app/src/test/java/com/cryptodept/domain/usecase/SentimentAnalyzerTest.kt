package com.cryptodept.domain.usecase

import com.cryptodept.data.api.RssNewsParser
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SentimentAnalyzerTest {
    private lateinit var rssParser: RssNewsParser
    private lateinit var analyzer: SentimentAnalyzer

    @Before
    fun setup() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any<String>(), any<String>()) } returns 0
        every { android.util.Log.i(any<String>(), any<String>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>()) } returns 0

        rssParser = mockk()
        analyzer = SentimentAnalyzer(rssParser)
    }

    @Test
    fun `analyzeCoin returns Bullish for positive headlines`() =
        runTest {
            val symbol = "BTC"
            val headlines =
                listOf(
                    RssNewsParser.RssItem(
                        title = "BTC surges to new ATH",
                        description = "Bullish growth",
                        link = "",
                        pubDate = "",
                        source = "Test",
                    ),
                    RssNewsParser.RssItem(
                        title = "Institutional adoption of Bitcoin",
                        description = "Strong support",
                        link = "",
                        pubDate = "",
                        source = "Test",
                    ),
                )
            coEvery { rssParser.fetchAllSources() } returns headlines
            coEvery { rssParser.parseUrl(any()) } returns emptyList()

            val result = analyzer.analyzeCoin(symbol)
            assertThat(result.verdict).isEqualTo(SentimentVerdict.STRONGLY_BULLISH)
            assertThat(result.bullishPercent).isGreaterThan(result.bearishPercent)
        }

    @Test
    fun `analyzeCoin returns Bearish for negative headlines`() =
        runTest {
            val symbol = "ETH"
            val headlines =
                listOf(
                    RssNewsParser.RssItem(
                        title = "ETH crash after major hack",
                        description = "Market collapse",
                        link = "",
                        pubDate = "",
                        source = "Test",
                    ),
                    RssNewsParser.RssItem(
                        title = "Bearish dump continues for Ethereum",
                        description = "Weak support",
                        link = "",
                        pubDate = "",
                        source = "Test",
                    ),
                )
            coEvery { rssParser.fetchAllSources() } returns headlines
            coEvery { rssParser.parseUrl(any()) } returns emptyList()

            val result = analyzer.analyzeCoin(symbol)
            assertThat(result.verdict).isEqualTo(SentimentVerdict.STRONGLY_BEARISH)
            assertThat(result.bearishPercent).isGreaterThan(result.bullishPercent)
        }

    @Test
    fun `calculatePulse handles neutral sentiment`() {
        val result = SentimentResult("SOL", 10, 10, 80, 10, SentimentVerdict.NEUTRAL)
        val pulse = analyzer.calculatePulse(result)
        assertThat(pulse).isEqualTo(50)
    }

    @Test
    fun `analyzeCoin uses cache`() =
        runTest {
            val symbol = "BNB"
            val headlines =
                listOf(RssNewsParser.RssItem(title = "BNB is bullish", description = "", link = "", pubDate = "", source = "Test"))
            coEvery { rssParser.fetchAllSources() } returns headlines
            coEvery { rssParser.parseUrl(any()) } returns emptyList()

            // First call
            analyzer.analyzeCoin(symbol)

            // Second call should not trigger another fetch
            analyzer.analyzeCoin(symbol)

            io.mockk.coVerify(exactly = 1) { rssParser.fetchAllSources() }
        }
}
