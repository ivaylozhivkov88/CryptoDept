package com.cryptodept.data.api

import com.cryptodept.BuildConfig
import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApiService {
    @GET("posts/")
    suspend fun getCryptoPanicNews(
        @Query("auth_token") token: String = BuildConfig.CRYPTOPANIC_API_KEY,
        @Query("public") public: Boolean = true,
        @Query("kind") kind: String = "news",
        @Query("currencies") currencies: String = "BTC,ETH",
    ): CryptoPanicResponse
}

data class CryptoPanicResponse(
    val count: Int,
    val results: List<CryptoPanicNewsItem>,
)

data class CryptoPanicNewsItem(
    val id: Int,
    val kind: String,
    val domain: String,
    val title: String,
    val url: String,
    @SerializedName("created_at") val createdAt: String,
    val votes: CryptoPanicVotes,
    val currencies: List<CryptoPanicCurrency>?,
)

data class CryptoPanicVotes(
    val negative: Int,
    val positive: Int,
    val important: Int,
    val liked: Int,
    val toxic: Int,
)

data class CryptoPanicCurrency(
    val code: String,
    val title: String,
    val slug: String,
    val url: String,
)
