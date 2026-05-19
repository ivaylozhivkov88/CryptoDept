package com.cryptodept.data.api.binance

import retrofit2.http.GET
import retrofit2.http.Query
import com.google.gson.annotations.SerializedName

/**
 * Binance Futures public API.
 */
interface BinanceFuturesService {
    
    @GET("fapi/v1/premiumIndex")
    suspend fun getPremiumIndex(
        @Query("symbol") symbol: String? = null,
    ): List<BinancePremiumIndexDto>
    
    @GET("futures/data/openInterestHist")
    suspend fun getOpenInterestHistory(
        @Query("symbol") symbol: String,
        @Query("period") period: String = "1h",
        @Query("limit") limit: Int = 24,
    ): List<BinanceOpenInterestHistDto>
    
    @GET("futures/data/globalLongShortAccountRatio")
    suspend fun getLongShortRatio(
        @Query("symbol") symbol: String,
        @Query("period") period: String = "1h",
        @Query("limit") limit: Int = 1,
    ): List<BinanceLongShortDto>
}

data class BinancePremiumIndexDto(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("markPrice") val markPrice: String,
    @SerializedName("lastFundingRate") val lastFundingRate: String,
    @SerializedName("nextFundingTime") val nextFundingTime: Long,
)

data class BinanceOpenInterestHistDto(
    @SerializedName("sumOpenInterest") val sumOpenInterest: String,
    @SerializedName("sumOpenInterestValue") val sumOpenInterestValue: String,
    @SerializedName("timestamp") val timestamp: Long,
)

data class BinanceLongShortDto(
    @SerializedName("longAccount") val longAccount: String,
    @SerializedName("shortAccount") val shortAccount: String,
    @SerializedName("longShortRatio") val longShortRatio: String,
)
