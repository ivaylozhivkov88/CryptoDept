package com.cryptodept.data.api

import retrofit2.http.GET
import retrofit2.http.Path

interface DexScreenerApi {
    @GET("latest/dex/pairs/{chainId}/{pairAddress}")
    suspend fun getPair(
        @Path("chainId") chainId: String,
        @Path("pairAddress") pairAddress: String
    ): DexPairResponse

    @GET("latest/dex/search?q={query}")
    suspend fun searchPairs(
        @Path("query") query: String
    ): DexPairResponse
}

data class DexPairResponse(
    val schemaVersion: String,
    val pairs: List<DexPair>?
)

data class DexPair(
    val chainId: String,
    val dexId: String,
    val url: String,
    val pairAddress: String,
    val baseToken: TokenInfo,
    val quoteToken: TokenInfo,
    val priceUsd: String?,
    val priceNative: String?,
    val liquidity: LiquidityInfo?,
    val volume: VolumeInfo?,
    val priceChange: PriceChangeInfo?
)

data class TokenInfo(
    val address: String,
    val name: String,
    val symbol: String
)

data class LiquidityInfo(
    val usd: Double?,
    val base: Double?,
    val quote: Double?
)

data class VolumeInfo(
    val h24: Double?,
    val h6: Double?,
    val h1: Double?,
    val m5: Double?
)

data class PriceChangeInfo(
    val m5: Double?,
    val h1: Double?,
    val h6: Double?,
    val h24: Double?
)
