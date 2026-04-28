package com.cryptodept.data.api

import retrofit2.http.GET
import retrofit2.http.Path

interface CoinCapApi {
    @GET("assets/{id}")
    suspend fun getAsset(
        @Path("id") id: String // Example: bitcoin
    ): CoinCapResponse
}

data class CoinCapResponse(
    val data: CoinCapAssetData,
    val timestamp: Long
)

data class CoinCapAssetData(
    val id: String,
    val symbol: String,
    val priceUsd: String
) {
    val lastPrice: Double? get() = priceUsd.toDoubleOrNull()
}
