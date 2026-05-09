package com.cryptodept.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CoinGeckoApi {
    @GET("simple/price")
    suspend fun getSimplePrice(
        @Query("ids") ids: String,
        @Query("vs_currencies") vsCurrencies: String = "usd",
        @Query("include_market_cap") includeMarketCap: Boolean = true,
        @Query("include_24hr_vol") include24hrVol: Boolean = true,
        @Query("include_24hr_change") include24hrChange: Boolean = true,
        @Query("include_last_updated_at") includeLastUpdatedAt: Boolean = true,
    ): Map<String, Map<String, Double>>

    @GET("coins/markets")
    suspend fun getCoinMarkets(
        @Query("vs_currency") vsCurrency: String = "usd",
        @Query("ids") ids: String? = null,
        @Query("order") order: String = "market_cap_desc",
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1,
        @Query("sparkline") sparkline: Boolean = true,
        @Query("price_change_percentage") priceChangePercentage: String = "24h",
    ): List<CoinMarketResponse>

    @GET("coins/{id}/ohlc")
    suspend fun getCoinOHLC(
        @Path("id") id: String,
        @Query("vs_currency") vsCurrency: String = "usd",
        @Query("days") days: String,
    ): List<List<Double>>

    @GET("coins/{id}")
    suspend fun getCoinDetail(
        @Path("id") coinId: String,
        @Query("localization") localization: Boolean = false,
        @Query("tickers") tickers: Boolean = true,
        @Query("market_data") marketData: Boolean = true,
        @Query("community_data") communityData: Boolean = false,
        @Query("developer_data") developerData: Boolean = false,
        @Query("sparkline") sparkline: Boolean = true,
    ): CoinDetailResponse

    @GET("global")
    suspend fun getGlobalData(): GlobalDataResponse

    @GET("search/trending")
    suspend fun getTrending(): TrendingResponse

    @GET("news")
    suspend fun getNews(): CoinGeckoNewsResponse
}

data class CoinGeckoNewsResponse(
    val data: List<CoinGeckoNewsItem>,
)

data class CoinGeckoNewsItem(
    val title: String,
    val description: String,
    val url: String,
    @SerializedName("updated_at") val updatedAt: Long,
    @SerializedName("news_site") val newsSource: String,
    @SerializedName("thumb_2x") val thumb: String?,
)

data class CoinMarketResponse(
    val id: String,
    val symbol: String,
    val name: String,
    val image: String,
    val current_price: Double,
    val market_cap: Double,
    val market_cap_rank: Int,
    val fully_diluted_valuation: Double?,
    val total_volume: Double,
    val high_24h: Double,
    val low_24h: Double,
    val price_change_24h: Double,
    val price_change_percentage_24h: Double,
    val market_cap_change_24h: Double,
    val market_cap_change_percentage_24h: Double,
    val circulating_supply: Double,
    val total_supply: Double?,
    val max_supply: Double?,
    val ath: Double,
    val ath_change_percentage: Double,
    val ath_date: String,
    val atl: Double,
    val atl_change_percentage: Double,
    val atl_date: String,
    val last_updated: String,
    val sparkline_in_7d: SparklineResponse?,
)

data class SparklineResponse(
    val price: List<Double>,
)

data class CoinDetailResponse(
    val id: String,
    val symbol: String,
    val name: String,
    val description: Map<String, String>?,
    val links: CoinLinks?,
    @SerializedName("market_data") val marketData: MarketData?,
    val tickers: List<TickerDetail>?,
)

data class CoinLinks(
    val homepage: List<String>?,
    @SerializedName("blockchain_site") val blockchainSite: List<String>?,
    @SerializedName("official_forum_url") val officialForumUrl: List<String>?,
    @SerializedName("subreddit_url") val subredditUrl: String?,
)

data class MarketData(
    @SerializedName("current_price") val currentPrice: Map<String, Double>,
    @SerializedName("market_cap") val marketCap: Map<String, Double>,
    @SerializedName("total_volume") val totalVolume: Map<String, Double>,
    @SerializedName("high_24h") val high24h: Map<String, Double>?,
    @SerializedName("low_24h") val low24h: Map<String, Double>?,
    @SerializedName("price_change_percentage_24h") val priceChangePercentage24h: Double,
    @SerializedName("sparkline_7d") val sparkline7d: SparklineResponse?,
)

data class TickerDetail(
    val base: String,
    val target: String,
    val market: MarketInfo,
    val last: Double,
    val volume: Double,
    @SerializedName("trade_url") val tradeUrl: String?,
)

data class MarketInfo(
    val name: String,
    val identifier: String,
)

data class GlobalDataResponse(
    val data: GlobalData,
)

data class GlobalData(
    @SerializedName("active_cryptocurrencies") val activeCryptocurrencies: Int,
    @SerializedName("total_market_cap") val totalMarketCap: Map<String, Double>,
    @SerializedName("total_volume") val totalVolume: Map<String, Double>,
    @SerializedName("market_cap_percentage") val marketCapPercentage: Map<String, Double>,
    @SerializedName("market_cap_change_percentage_24h_usd") val marketCapChangePercentage24hUsd: Double,
)

data class TrendingResponse(
    val coins: List<TrendingCoinItem>,
)

data class TrendingCoinItem(
    val item: TrendingCoin,
)

data class TrendingCoin(
    val id: String,
    val name: String,
    val symbol: String,
    @SerializedName("market_cap_rank") val marketCapRank: Int,
    val thumb: String,
    val score: Int,
)
