package com.cryptodept.data.api

import com.cryptodept.domain.model.NewsItem
import com.cryptodept.domain.model.NewsSentiment
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class RedditClient
    @Inject
    constructor(
        @Named("PublicClient") private val okHttpClient: OkHttpClient,
        private val gson: Gson,
    ) {
        suspend fun fetchCryptoReddit(): List<NewsItem> =
            withContext(Dispatchers.IO) {
                val request =
                    Request
                        .Builder()
                        .url("https://www.reddit.com/r/cryptocurrency.json?limit=25")
                        .header("User-Agent", "CryptoDeptApp/1.0 (by /u/CryptoDeptAdmin)")
                        .build()

                try {
                    val response = okHttpClient.newCall(request).execute()
                    val body = response.body?.string() ?: return@withContext emptyList()
                    response.close()

                    val json = gson.fromJson(body, JsonObject::class.java)
                    val children = json.getAsJsonObject("data").getAsJsonArray("children")

                    children.map { element ->
                        val post = element.asJsonObject.getAsJsonObject("data")
                        val title = post.get("title").asString
                        val url = "https://reddit.com" + post.get("permalink").asString
                        val author = post.get("author").asString
                        val timestamp = post.get("created_utc").asLong * 1000

                        val sentiment =
                            when {
                                title.contains("pump", true) || title.contains("bull", true) -> NewsSentiment.BULLISH
                                title.contains("dump", true) || title.contains("bear", true) -> NewsSentiment.BEARISH
                                else -> NewsSentiment.NEUTRAL
                            }

                        NewsItem(
                            id = post.get("id").asString,
                            title = title,
                            url = url,
                            source = "Reddit /r/cryptocurrency ($author)",
                            publishedAt = timestamp,
                            sentiment = sentiment,
                            currencies = emptyList<String>().toImmutableList(),
                        )
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            }
    }
