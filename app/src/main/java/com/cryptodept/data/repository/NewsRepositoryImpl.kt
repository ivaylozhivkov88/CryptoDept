package com.cryptodept.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.cryptodept.BuildConfig
import com.cryptodept.data.api.NewsApiService
import com.cryptodept.data.db.NewsDao
import com.cryptodept.data.db.NewsEntity
import com.cryptodept.domain.model.*
import com.cryptodept.domain.repository.NewsRepository
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepositoryImpl
    @Inject
    constructor(
        private val newsApi: NewsApiService,
        private val coinGeckoApi: com.cryptodept.data.api.CoinGeckoApi,
        private val rssParser: com.cryptodept.data.api.RssNewsParser,
        private val redditClient: com.cryptodept.data.api.RedditClient,
        private val sentimentScorer: com.cryptodept.domain.algo.LocalSentimentScorer,
        private val newsDao: NewsDao,
    ) : NewsRepository {

        override fun getNews(currencies: String?): Flow<List<NewsItem>> {
            return newsDao.getLatestNews(50).map { entities ->
                entities.map { it.toDomain() }
            }
        }

        override fun getNewsPagingData(): Flow<PagingData<NewsItem>> =
            Pager(
                config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                pagingSourceFactory = { newsDao.getPagingSource() },
            ).flow.map { pagingData ->
                pagingData.map { it.toDomain() }
            }

        override suspend fun refreshNews(currencies: String?): Result<Unit> = withContext(Dispatchers.IO) {
            try {
                val cryptoPanicDeferred = async {
                    if (BuildConfig.CRYPTOPANIC_API_KEY.isNotBlank()) {
                        runCatching {
                            val query = currencies ?: "BTC,ETH,XRP,SOL,ADA"
                            newsApi.getCryptoPanicNews(currencies = query).results.map { res ->
                                val sentiment = when {
                                    res.votes.positive > res.votes.negative -> NewsSentiment.BULLISH
                                    res.votes.negative > res.votes.positive -> NewsSentiment.BEARISH
                                    else -> NewsSentiment.NEUTRAL
                                }
                                NewsItem(
                                    id = res.id.toString(),
                                    title = res.title,
                                    url = res.url,
                                    source = res.domain,
                                    publishedAt = parseIsoDate(res.createdAt),
                                    sentiment = sentiment,
                                    currencies = (res.currencies?.map { it.code } ?: emptyList()).toImmutableList(),
                                )
                            }
                        }.getOrDefault(emptyList())
                    } else emptyList()
                }

                val coinGeckoDeferred = async {
                    runCatching {
                        coinGeckoApi.getNews().data.map { res ->
                            NewsItem(
                                id = res.url.hashCode().toString(),
                                title = res.title,
                                url = res.url,
                                source = res.newsSource,
                                publishedAt = res.updatedAt * 1000,
                                sentiment = sentimentScorer.analyze(res.title),
                                currencies = detectCurrencies(res.title).toImmutableList(),
                            )
                        }
                    }.getOrDefault(emptyList())
                }

                val rssDeferred = async {
                    runCatching {
                        rssParser.fetchAllSources().map { rss ->
                            NewsItem(
                                id = rss.link.hashCode().toString(),
                                title = rss.title,
                                url = rss.link,
                                source = rss.source,
                                publishedAt = parseRssDate(rss.pubDate),
                                sentiment = sentimentScorer.analyze(rss.title),
                                currencies = detectCurrencies(rss.title).toImmutableList(),
                            )
                        }
                    }.getOrDefault(emptyList())
                }

                val redditDeferred = async {
                    runCatching { redditClient.fetchCryptoReddit() }.getOrDefault(emptyList())
                }

                val results = awaitAll(cryptoPanicDeferred, coinGeckoDeferred, rssDeferred, redditDeferred)
                val combined = results.flatten().distinctBy { it.url }.sortedByDescending { it.publishedAt }

                if (combined.isNotEmpty()) {
                    newsDao.insertNews(combined.map { NewsEntity.fromDomain(it) })
                    newsDao.deleteOldNews(System.currentTimeMillis() - 7 * 24 * 3600 * 1000)
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        private fun parseIsoDate(dateStr: String): Long =
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                sdf.parse(dateStr)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }

        private fun detectCurrencies(text: String): List<String> {
            val detected = mutableListOf<String>()
            val upper = text.uppercase()
            if (upper.contains("BTC") || upper.contains("BITCOIN")) detected.add("BTC")
            if (upper.contains("ETH") || upper.contains("ETHEREUM")) detected.add("ETH")
            if (upper.contains("XRP") || upper.contains("RIPPLE")) detected.add("XRP")
            if (upper.contains("SOL") || upper.contains("SOLANA")) detected.add("SOL")
            if (upper.contains("ADA") || upper.contains("CARDANO")) detected.add("ADA")
            if (upper.contains("DOGE")) detected.add("DOGE")
            return detected
        }

        private fun parseRssDate(dateStr: String): Long =
            try {
                SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US).parse(dateStr)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
    }
