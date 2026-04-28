package com.cryptodept.data.api

import retrofit2.http.GET
import retrofit2.http.Path

interface CoinbaseApi {
    @GET("products/{product_id}/ticker")
    suspend fun getProductTicker(
        @Path("product_id") productId: String // Example: BTC-USD
    ): CoinbaseTickerResponse
}

data class CoinbaseTickerResponse(
    val price: String,
    val volume: String,
    val time: String
) {
    val lastPrice: Double? get() = price.toDoubleOrNull()
}
