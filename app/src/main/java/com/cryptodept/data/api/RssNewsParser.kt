package com.cryptodept.data.api

import android.util.Log
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class RssNewsParser
    @Inject
    constructor(
        @Named("PublicClient") private val okHttpClient: OkHttpClient,
    ) {
        data class RssItem(
            val title: String,
            val link: String,
            val pubDate: String,
            val description: String,
            val source: String,
            val category: String = "",
        )

        data class RssSource(
            val name: String,
            val url: String,
            val category: String,
        )

        private val RSS_SOURCES =
            listOf(
                RssSource("CryptoPanic", "https://cryptopanic.com/news/rss/", "Aggregator"),
                RssSource("CoinTelegraph", "https://cointelegraph.com/rss", "General"),
                RssSource("The Block", "https://www.theblock.co/rss.xml", "Institutional"),
                RssSource("Decrypt", "https://decrypt.co/feed", "General"),
                RssSource("Bitcoin Mag", "https://bitcoinmagazine.com/feed", "Bitcoin"),
                RssSource("CryptoSlate", "https://cryptoslate.com/feed/", "General"),
                RssSource("NewsBTC", "https://www.newsbtc.com/feed/", "General"),
                RssSource("CoinDesk", "https://www.coindesk.com/arc/outboundfeeds/rss/", "General"),
            )

        suspend fun fetchAllSources(): List<RssItem> = coroutineScope {
            val deferredItems = RSS_SOURCES.map { source ->
                async(Dispatchers.IO) {
                    try {
                        withTimeout(8000) {
                            fetchRssFeed(source)
                        }
                    } catch (e: Exception) {
                        Log.e("RSS_ERROR", "Failed ${source.name}: ${e.message}")
                        emptyList<RssItem>()
                    }
                }
            }
            deferredItems.awaitAll().flatten().sortedByDescending { parseRssDate(it.pubDate) }
        }

        suspend fun parseUrl(url: String): List<RssItem> =
            withContext(Dispatchers.IO) {
                fetchRssFeed(RssSource("Generic", url, "General"))
            }

        private suspend fun fetchRssFeed(source: RssSource): List<RssItem> {
            val request =
                Request
                    .Builder()
                    .url(source.url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()

            val items = mutableListOf<RssItem>()

            try {
                val response = okHttpClient.newCall(request).execute()
                val xml = response.body?.string() ?: return emptyList()
                response.close()

                val factory = XmlPullParserFactory.newInstance()
                val parser = factory.newPullParser()
                parser.setInput(StringReader(xml))

                var currentItem: RssItem? = null
                var eventType = parser.eventType

                var title = ""
                var link = ""
                var pubDate = ""
                var description = ""

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    val tagName = parser.name
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            if (tagName == "item") {
                                title = ""
                                link = ""
                                pubDate = ""
                                description = ""
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (tagName == "item" && title.isNotEmpty()) {
                                items.add(RssItem(title, link, pubDate, description, source.name, source.category))
                            }
                        }
                        XmlPullParser.TEXT -> {} // Не ни трябва тук
                    }

                    // Използваме nextText() за сигурност вътре в START_TAG
                    if (eventType == XmlPullParser.START_TAG) {
                        when (tagName) {
                            "title" ->
                                title =
                                    try {
                                        parser.nextText()
                                    } catch (e: Exception) {
                                        ""
                                    }
                            "link" ->
                                link =
                                    try {
                                        parser.nextText()
                                    } catch (e: Exception) {
                                        ""
                                    }
                            "pubDate" ->
                                pubDate =
                                    try {
                                        parser.nextText()
                                    } catch (e: Exception) {
                                        ""
                                    }
                            "description" ->
                                description =
                                    try {
                                        parser.nextText()
                                    } catch (e: Exception) {
                                        ""
                                    }
                        }
                    }
                    eventType = parser.next()
                }
            } catch (e: Exception) {
                Log.e("RSS_PARSER", "Error ${source.name}: ${e.message}")
            }
            return items.take(25)
        }

        private fun parseRssDate(dateStr: String): Long =
            try {
                java.text
                    .SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", java.util.Locale.ENGLISH)
                    .parse(dateStr)
                    ?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
    }
