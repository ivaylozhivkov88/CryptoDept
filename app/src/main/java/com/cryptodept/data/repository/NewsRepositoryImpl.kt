package com.cryptodept.data.repository

import com.cryptodept.data.api.NewsApiService
import com.cryptodept.BuildConfig
import com.cryptodept.domain.model.*
import com.cryptodept.domain.repository.NewsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepositoryImpl @Inject constructor(
    private val newsApi: NewsApiService,
    private val coinGeckoApi: com.cryptodept.data.api.CoinGeckoApi,
    private val rssParser: com.cryptodept.data.api.RssNewsParser
) : NewsRepository {

    private val _news = MutableStateFlow<List<NewsItem>>(emptyList())

    override fun getNews(currencies: String?): Flow<List<NewsItem>> = _news.asStateFlow()

    override suspend fun refreshNews(currencies: String?): Result<Unit> {
        return try {
            val response = if (BuildConfig.CRYPTOPANIC_API_KEY.isNotBlank()) {
                val query = currencies ?: "BTC,ETH,XRP,SOL,ADA,DOT,LINK,LTC,AVAX,TRX"
                newsApi.getCryptoPanicNews(currencies = query).results.map { res ->
                    val sentiment = when {
                        res.votes.positive > res.votes.negative -> NewsSentiment.BULLISH
                        res.votes.negative > res.votes.positive -> NewsSentiment.BEARISH
                        else -> NewsSentiment.NEUTRAL
                    }

                    val timestamp = try {
                        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                        sdf.timeZone = TimeZone.getTimeZone("UTC")
                        sdf.parse(res.createdAt)?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }

                    NewsItem(
                        id = res.id.toString(),
                        title = res.title,
                        url = res.url,
                        source = res.domain,
                        publishedAt = timestamp,
                        sentiment = sentiment,
                        currencies = res.currencies?.map { it.code } ?: emptyList()
                    )
                }
            } else {
                emptyList() // No API key, go to RSS/CG
            }

            val finalNews = if (response.isEmpty()) {
                // Try CoinGecko news as second fallback
                val cgNews = runCatching { coinGeckoApi.getNews().data }.getOrElse { emptyList() }
                if (cgNews.isNotEmpty()) {
                    cgNews.map { res ->
                        NewsItem(
                            id = res.url.hashCode().toString(),
                            title = res.title,
                            url = res.url,
                            source = res.newsSource,
                            publishedAt = res.updatedAt * 1000,
                            sentiment = NewsSentiment.NEUTRAL,
                            currencies = emptyList()
                        )
                    }
                } else {
                    // Final fallback: RSS
                    val rssItems = rssParser.fetchAllSources()
                    rssItems.map { rss ->
                        val sentiment = when {
                            rss.title.contains("surge", true) || rss.title.contains("bull", true) || rss.title.contains("gain", true) -> NewsSentiment.BULLISH
                            rss.title.contains("crash", true) || rss.title.contains("bear", true) || rss.title.contains("drop", true) -> NewsSentiment.BEARISH
                            else -> NewsSentiment.NEUTRAL
                        }
                        
                        val detectedCurrencies = mutableListOf<String>()
                        val titleUpper = rss.title.uppercase()
                        if (titleUpper.contains("BTC") || titleUpper.contains("BITCOIN")) detectedCurrencies.add("BTC")
                        if (titleUpper.contains("ETH") || titleUpper.contains("ETHEREUM")) detectedCurrencies.add("ETH")
                        if (titleUpper.contains("XRP") || titleUpper.contains("RIPPLE")) detectedCurrencies.add("XRP")
                        if (titleUpper.contains("SOL") || titleUpper.contains("SOLANA")) detectedCurrencies.add("SOL")
                        if (titleUpper.contains("ADA") || titleUpper.contains("CARDANO")) detectedCurrencies.add("ADA")
                        if (titleUpper.contains("DOGE")) detectedCurrencies.add("DOGE")
                        if (detectedCurrencies.isEmpty() && rss.category.isNotBlank()) detectedCurrencies.add(rss.category)

                        NewsItem(
                            id = rss.link.hashCode().toString(),
                            title = rss.title,
                            url = rss.link,
                            source = rss.source,
                            publishedAt = parseRssDate(rss.pubDate),
                            sentiment = sentiment,
                            currencies = detectedCurrencies
                        )
                    }
                }
            } else {
                response
            }
            
            _news.value = finalNews.sortedByDescending { it.publishedAt }
            if (_news.value.isEmpty()) {
                // LAST DITCH EFFORT: Hardcoded "System News" if everything fails
                _news.value = listOf(
                    NewsItem("sys1", "TERMINAL_INFO: GLOBAL DATA STREAMS ARE CURRENTLY CONGESTED.", "", "SYSTEM", System.currentTimeMillis(), NewsSentiment.NEUTRAL, listOf("SYSTEM")),
                    NewsItem("sys2", "MARKET_WATCH: BTC AND ETH VOLATILITY INCREASING. MONITOR ENTRIES.", "", "SYSTEM", System.currentTimeMillis() - 3600000, NewsSentiment.BULLISH, listOf("BTC", "ETH"))
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseRssDate(dateStr: String): Long {
        return try {
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US).parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
