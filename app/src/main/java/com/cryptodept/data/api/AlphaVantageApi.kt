package com.cryptodept.data.api

import com.cryptodept.BuildConfig
import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface AlphaVantageApi {

    // S&P 500 (SPY ETF)
    @GET("query")
    suspend fun getQuote(
        @Query("function") function: String = "GLOBAL_QUOTE",
        @Query("symbol") symbol: String,  // "SPY", "GLD", "UUP" (DXY proxy)
        @Query("apikey") apiKey: String = BuildConfig.ALPHA_VANTAGE_API_KEY
    ): AlphaVantageQuoteResponseDto

    // Crypto RSI от Alpha Vantage (алтернативен source)
    @GET("query")
    suspend fun getCryptoRSI(
        @Query("function") function: String = "RSI",
        @Query("symbol") symbol: String,    // "BTC", "ETH"
        @Query("market") market: String = "USD",
        @Query("interval") interval: String = "daily",
        @Query("time_period") timePeriod: Int = 14,
        @Query("series_type") seriesType: String = "close",
        @Query("apikey") apiKey: String = BuildConfig.ALPHA_VANTAGE_API_KEY
    ): AlphaVantageRSIResponseDto
}

// Base URL: "https://www.alphavantage.co/"

data class AlphaVantageQuoteResponseDto(
    @SerializedName("Global Quote") val globalQuote: GlobalQuoteDto?
)

data class GlobalQuoteDto(
    @SerializedName("01. symbol") val symbol: String,
    @SerializedName("05. price") val price: String,
    @SerializedName("09. change") val change: String,
    @SerializedName("10. change percent") val changePercent: String
)

data class AlphaVantageRSIResponseDto(
    @SerializedName("Technical Analysis: RSI") val rsiData: Map<String, RsiValueDto>?
)

data class RsiValueDto(
    @SerializedName("RSI") val rsi: String
)
