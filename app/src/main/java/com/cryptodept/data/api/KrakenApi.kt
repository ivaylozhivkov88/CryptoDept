package com.cryptodept.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface KrakenApi {
    @GET("Ticker")
    suspend fun getTicker(
        @Query("pair") pair: String, // Example: XBTUSD,ETHUSD
    ): KrakenResponse
}

data class KrakenResponse(
    val error: List<String>,
    val result: Map<String, KrakenTickerInfo>,
)

data class KrakenTickerInfo(
    val c: List<String>, // Last trade: [price, whole lot volume]
) {
    val lastPrice: Double? get() = c.firstOrNull()?.toDoubleOrNull()
}
