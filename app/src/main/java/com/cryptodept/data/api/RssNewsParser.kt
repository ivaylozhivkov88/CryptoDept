package com.cryptodept.data.api

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

@Singleton
class RssNewsParser @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    data class RssItem(
        val title: String,
        val link: String,
        val pubDate: String,
        val description: String,
        val source: String,
        val category: String = ""
    )

    private val RSS_SOURCES = listOf(
        RssSource("CoinTelegraph",  "https://cointelegraph.com/rss",          "General"),
        RssSource("The Block",      "https://www.theblock.co/rss.xml",         "Institutional"),
        RssSource("Decrypt",        "https://decrypt.co/feed",                 "General"),
        RssSource("Bitcoin Mag",    "https://bitcoinmagazine.com/feed",        "Bitcoin"),
        RssSource("CryptoSlate",    "https://cryptoslate.com/feed/",           "General"),
        RssSource("Blockworks",     "https://blockworks.co/feed",              "Professional"),
        RssSource("DL News",        "https://www.dlnews.com/arc/outboundfeeds/rss/", "DeFi"),
        RssSource("CoinDesk",       "https://www.coindesk.com/arc/outboundfeeds/rss/", "General"),
        RssSource("NewsBTC",        "https://www.newsbtc.com/feed/",           "Technical")
    )

    data class RssSource(val name: String, val url: String, val category: String)

    suspend fun fetchAllSources(): List<RssItem> = withContext(Dispatchers.IO) {
        val allItems = mutableListOf<RssItem>()

        RSS_SOURCES.forEach { source ->
            try {
                val items = fetchRssFeed(source)
                allItems.addAll(items)
                delay(300L) // Малка пауза между заявките
            } catch (e: Exception) {
                Log.e("CryptoDept_RSS", "Failed to fetch ${source.name}: ${e.message}")
            }
        }

        // Сортирай по дата (най-ново първо)
        allItems.sortedByDescending { parseRssDate(it.pubDate) }
    }

    private suspend fun fetchRssFeed(source: RssSource): List<RssItem> {
        val request = Request.Builder().url(source.url).build()
        val response = okHttpClient.newCall(request).execute()
        val xml = response.body?.string() ?: return emptyList()

        val items = mutableListOf<RssItem>()
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(StringReader(xml))

        var title = ""; var link = ""; var pubDate = ""; var description = ""
        var inItem = false
        var currentTag = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (currentTag == "item") { inItem = true; title = ""; link = ""; pubDate = ""; description = "" }
                }
                XmlPullParser.TEXT -> {
                    if (inItem) {
                        when (currentTag) {
                            "title"       -> title = parser.text.trim()
                            "link"        -> if (link.isEmpty()) link = parser.text.trim()
                            "pubDate"     -> pubDate = parser.text.trim()
                            "description" -> if (description.isEmpty()) description = parser.text.trim()
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "item" && inItem && title.isNotEmpty()) {
                        items.add(RssItem(title, link, pubDate, description, source.name, source.category))
                        inItem = false
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }

        return items.take(10) // Максимум 10 статии от всеки source
    }

    private fun parseRssDate(dateStr: String): Long {
        return try {
            java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", java.util.Locale.ENGLISH)
                .parse(dateStr)?.time ?: 0L
        } catch (e: Exception) { 0L }
    }
}
