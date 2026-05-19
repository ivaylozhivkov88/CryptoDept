package com.cryptodept.data.api.defillama

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

/**
 * DefiLlama API — free yield and TVL data.
 */
interface DefiLlamaService {
    
    @GET("protocols")
    suspend fun getAllProtocols(): List<DefiLlamaProtocolDto>
    
    @GET
    suspend fun getYieldPools(@Url url: String = "https://yields.llama.fi/pools"): DefiLlamaYieldResponse
}

data class DefiLlamaProtocolDto(
    val id: String,
    val name: String,
    val symbol: String?,
    val category: String?,
    val chains: List<String>?,
    val tvl: Double,
    val change_1d: Double?,
)

data class DefiLlamaYieldResponse(
    val data: List<DefiLlamaPoolDto>,
)

data class DefiLlamaPoolDto(
    val pool: String,
    val chain: String,
    val project: String,
    val symbol: String,
    val tvlUsd: Double,
    val apy: Double?,
    val stablecoin: Boolean,
    val ilRisk: String?,
)
