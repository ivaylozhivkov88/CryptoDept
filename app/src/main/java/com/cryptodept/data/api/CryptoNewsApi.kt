package com.cryptodept.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface CryptoNewsApi {
    @GET("api/news")
    suspend fun getLatestNews(
        @Query("limit") limit: Int = 20,
        @Query("category") category: String? = null
    ): List<CryptoNewsItem>
}

data class CryptoNewsItem(
    val title: String,
    val source: String,
    val url: String,
    @SerializedName("publishedAt") val publishedAt: String,
    val content: String?,
    val sentiment: NewsSentiment?
)

data class NewsSentiment(
    val score: Double,
    val label: String
)
