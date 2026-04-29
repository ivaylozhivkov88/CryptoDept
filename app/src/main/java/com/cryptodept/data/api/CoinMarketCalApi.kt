package com.cryptodept.data.api

import com.cryptodept.BuildConfig
import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface CoinMarketCalApi {

    @GET("v1/events")
    suspend fun getUpcomingEvents(
        @Header("x-api-key") apiKey: String = BuildConfig.COINMARKETCAL_API_KEY,
        @Query("coins") coins: String = "1,1027,52",  // BTC=1, ETH=1027, XRP=52
        @Query("max") max: Int = 20,
        @Query("dateRangeStart") dateStart: String? = null,  // "YYYY-MM-DD"
        @Query("dateRangeEnd") dateEnd: String? = null,
        @Query("sortBy") sortBy: String = "hot_score"
    ): CoinMarketCalResponseDto
}

// Base URL: "https://developers.coinmarketcal.com/"

data class CoinMarketCalResponseDto(
    @SerializedName("body") val events: List<CalendarEventDto>
)

data class CalendarEventDto(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: LocalizedTitleDto,
    @SerializedName("coins") val coins: List<CalendarCoinDto>,
    @SerializedName("date_event") val dateEvent: String,
    @SerializedName("created_date") val createdDate: String,
    @SerializedName("hot_score") val hotScore: Double,
    @SerializedName("proof") val proof: String?,
    @SerializedName("is_hot") val isHot: Boolean,
    @SerializedName("categories") val categories: List<CategoryDto>
)

data class LocalizedTitleDto(
    @SerializedName("en") val en: String
)

data class CalendarCoinDto(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("fullname") val fullname: String
)

data class CategoryDto(
    @SerializedName("name") val name: String  // "Hard fork", "Listing", "Partnership"
)
