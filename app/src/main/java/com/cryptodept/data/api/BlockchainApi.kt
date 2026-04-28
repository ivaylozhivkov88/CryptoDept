package com.cryptodept.data.api

import retrofit2.http.GET

interface BlockchainApi {
    @GET("stats")
    suspend fun getStats(): BlockchainStats

    @GET("q/getdifficulty")
    suspend fun getDifficulty(): Double
}

data class BlockchainStats(
    val hash_rate: Double,
    val mempool_count: Int,
    val trade_volume_btc: Double,
    val miners_revenue_usd: Double
)
