package com.cryptodept.data.api

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Service for fetching social engagement and sentiment metrics.
 * Placeholder for LunarCrush or custom backend analytics.
 */
interface SentimentApiService {
    @GET("social/pulse")
    suspend fun getSocialPulse(
        @Query("symbol") symbol: String,
    ): SocialPulseResponse
}

data class SocialPulseResponse(
    val symbol: String,
    val score: Int, // 0-100
    val mentions24h: Int,
    val positiveSent: Float,
    val negativeSent: Float,
    val dominantSource: String, // "TWITTER", "REDDIT", "TELEGRAM"
)
