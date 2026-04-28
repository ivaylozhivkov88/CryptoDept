package com.cryptodept.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface CryptoPanicApi {
    @GET("posts/")
    suspend fun getNews(
        @Query("auth_token") authToken: String,
        @Query("currencies") currencies: String? = null,
        @Query("kind") kind: String = "news",
        @Query("public") public: Boolean = true
    ): CryptoPanicResponse
}

data class CryptoPanicResponse(
    val count: Int,
    val results: List<CryptoPanicNewsItem>
)

data class CryptoPanicNewsItem(
    val id: Int,
    val kind: String,
    val domain: String,
    val title: String,
    val url: String,
    @SerializedName("created_at") val createdAt: String,
    val votes: CryptoPanicVotes
)

data class CryptoPanicVotes(
    val negative: Int,
    val positive: Int,
    val important: Int,
    val liked: Int,
    val toxic: Int
)
