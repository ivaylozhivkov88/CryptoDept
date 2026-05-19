package com.cryptodept.data.api.etherscan

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface EtherscanService {
    @GET("api?module=account&action=txlist")
    suspend fun getNormalTransactions(
        @Query("address") address: String,
        @Query("startblock") startBlock: Long = 0,
        @Query("endblock") endBlock: Long = 99999999,
        @Query("page") page: Int = 1,
        @Query("offset") offset: Int = 100,
        @Query("sort") sort: String = "desc",
        @Query("apikey") apiKey: String,
    ): EtherscanResponse<List<EtherscanTxDto>>
}

data class EtherscanResponse<T>(
    val status: String,
    val message: String,
    val result: T,
)

data class EtherscanTxDto(
    @SerializedName("hash") val hash: String,
    @SerializedName("from") val from: String,
    @SerializedName("to") val to: String,
    @SerializedName("value") val value: String,
    @SerializedName("timeStamp") val timestamp: String,
    @SerializedName("isError") val isError: String,
)
