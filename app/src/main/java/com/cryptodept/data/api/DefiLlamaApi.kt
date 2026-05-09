package com.cryptodept.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Url

interface DefiLlamaApi {
    @GET("protocols")
    suspend fun getProtocols(): List<ProtocolDto>

    @GET
    suspend fun getYields(
        @Url url: String = "https://yields.llama.fi/pools",
    ): YieldResponseDto
}

data class ProtocolDto(
    val id: String,
    val name: String,
    val symbol: String,
    val url: String,
    val description: String?,
    val logo: String,
    val tvl: Double,
    @SerializedName("change_1h") val tvlChange1h: Double?,
    @SerializedName("change_1d") val tvlChange1d: Double?,
    @SerializedName("change_7d") val tvlChange7d: Double?,
    val chain: String?,
    val category: String?,
)

data class YieldResponseDto(
    val status: String,
    val data: List<YieldPoolDto>,
)

data class YieldPoolDto(
    val protocol: String,
    val symbol: String,
    val tvlUsd: Double,
    val apy: Double,
    val chain: String,
)
