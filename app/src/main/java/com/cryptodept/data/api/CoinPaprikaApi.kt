package com.cryptodept.data.api

import retrofit2.http.GET
import retrofit2.http.Path

interface CoinPaprikaApi {
    @GET("tickers/{id}")
    suspend fun getTicker(
        @Path("id") id: String // Example: btc-bitcoin
    ): CoinPaprikaResponse
}

data class CoinPaprikaResponse(
    val id: String,
    val name: String,
    val symbol: String,
    val quotes: Map<String, CoinPaprikaQuote>
) {
    val lastPrice: Double? get() = quotes["USD"]?.price
}

data class CoinPaprikaQuote(
    val price: Double
)
