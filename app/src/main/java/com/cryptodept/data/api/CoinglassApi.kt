package com.cryptodept.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface CoinglassApi {
    // Aggregated funding rate от всички борси
    @GET("api/pro/v1/futures/funding-rate")
    suspend fun getAggregatedFunding(
        @Query("symbol") symbol: String, // "BTC", "ETH", "XRP"
    ): CoinglassFundingResponseDto

    // Liquidation heatmap данни
    @GET("api/pro/v1/futures/liquidation-heatmap")
    suspend fun getLiquidationHeatmap(
        @Query("symbol") symbol: String,
        @Query("range") range: String = "12h", // "1h","4h","12h","24h"
    ): LiquidationHeatmapDto

    // Global long/short liquidations (24h)
    @GET("api/pro/v1/futures/liquidation")
    suspend fun getGlobalLiquidations(
        @Query("symbol") symbol: String,
        @Query("time_type") timeType: String = "h1",
    ): GlobalLiquidationDto
}

// Base URL: "https://open-api.coinglass.com/"
// Header: "CG-API-KEY": BuildConfig.COINGLASS_API_KEY

data class CoinglassFundingResponseDto(
    @SerializedName("code") val code: String,
    @SerializedName("data") val data: List<ExchangeFundingDto>,
)

data class ExchangeFundingDto(
    @SerializedName("exchangeName") val exchangeName: String,
    @SerializedName("rate") val rate: Double,
    @SerializedName("nextFundingTime") val nextFundingTime: Long,
)

data class LiquidationHeatmapDto(
    @SerializedName("code") val code: String,
    @SerializedName("data") val data: HeatmapDataDto?,
)

data class HeatmapDataDto(
    @SerializedName("y") val pricelevels: List<Double>, // Ценови нива
    @SerializedName("longs") val longLiquidations: List<Double>, // USD стойност на long ликвидации
    @SerializedName("shorts") val shortLiquidations: List<Double>, // USD стойност на short ликвидации
)

data class GlobalLiquidationDto(
    @SerializedName("data") val data: LiquidationSummaryDto?,
)

data class LiquidationSummaryDto(
    @SerializedName("longVolUsd") val longVolUsd: Double, // Total long ликвидации в USD
    @SerializedName("shortVolUsd") val shortVolUsd: Double, // Total short ликвидации в USD
)
