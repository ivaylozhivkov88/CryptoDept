package com.cryptodept.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface BinanceFuturesApi {
    // Funding Rate за всички символи
    @GET("fapi/v1/premiumIndex")
    suspend fun getFundingRates(): List<FundingRateDto>

    // Funding Rate за конкретен символ
    @GET("fapi/v1/premiumIndex")
    suspend fun getFundingRate(
        @Query("symbol") symbol: String, // "BTCUSDT", "ETHUSDT"
    ): FundingRateDto

    // Open Interest
    @GET("fapi/v1/openInterest")
    suspend fun getOpenInterest(
        @Query("symbol") symbol: String,
    ): OpenInterestDto

    // Open Interest History (за тренд)
    @GET("futures/data/openInterestHist")
    suspend fun getOpenInterestHistory(
        @Query("symbol") symbol: String,
        @Query("period") period: String = "1h", // "5m","15m","30m","1h","2h","4h","6h","12h","1d"
        @Query("limit") limit: Int = 48,
    ): List<OpenInterestHistDto>

    // Long/Short Ratio
    @GET("futures/data/globalLongShortAccountRatio")
    suspend fun getLongShortRatio(
        @Query("symbol") symbol: String,
        @Query("period") period: String = "1h",
        @Query("limit") limit: Int = 24,
    ): List<LongShortRatioDto>

    // Top Liquidations (последните ликвидации)
    @GET("fapi/v1/forceOrders")
    suspend fun getLiquidations(
        @Query("symbol") symbol: String? = null,
        @Query("limit") limit: Int = 50,
    ): List<LiquidationDto>

    @GET("fapi/v1/klines")
    suspend fun getKlines(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String = "1d",
        @Query("limit") limit: Int = 100,
    ): List<List<Any>>
}

// Base URL: "https://fapi.binance.com/"

data class FundingRateDto(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("markPrice") val markPrice: String,
    @SerializedName("lastFundingRate") val lastFundingRate: String,
    @SerializedName("nextFundingTime") val nextFundingTime: Long,
)

data class OpenInterestDto(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("openInterest") val openInterest: String,
    @SerializedName("time") val time: Long,
)

data class OpenInterestHistDto(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("sumOpenInterest") val sumOpenInterest: String,
    @SerializedName("sumOpenInterestValue") val sumOpenInterestValue: String,
    @SerializedName("timestamp") val timestamp: Long,
)

data class LongShortRatioDto(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("longShortRatio") val longShortRatio: String,
    @SerializedName("longAccount") val longAccount: String,
    @SerializedName("shortAccount") val shortAccount: String,
    @SerializedName("timestamp") val timestamp: Long,
)

data class LiquidationDto(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("side") val side: String, // "BUY" или "SELL"
    @SerializedName("price") val price: String,
    @SerializedName("origQty") val origQty: String,
    @SerializedName("time") val time: Long,
)
