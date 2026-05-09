package com.cryptodept.data.api

import com.google.gson.JsonElement
import retrofit2.http.GET
import retrofit2.http.Query

interface EtherscanApi {
    @GET("api")
    suspend fun getGasOracle(
        @Query("module") module: String = "gastracker",
        @Query("action") action: String = "gasoracle",
        @Query("apikey") apiKey: String,
    ): EtherscanResponse

    @GET("api")
    suspend fun getTransactionList(
        @Query("module") module: String = "account",
        @Query("action") action: String = "txlist",
        @Query("address") address: String,
        @Query("startblock") startblock: Long = 0,
        @Query("endblock") endblock: Long = 99999999,
        @Query("page") page: Int = 1,
        @Query("offset") offset: Int = 10,
        @Query("sort") sort: String = "desc",
        @Query("apikey") apiKey: String,
    ): EtherscanResponse
}

data class EtherscanResponse(
    val status: String,
    val message: String,
    val result: JsonElement,
)

data class GasOracleResult(
    val SafeGasPrice: String,
    val ProposeGasPrice: String,
    val FastGasPrice: String,
)
