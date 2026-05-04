package com.cryptodept.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface WhaleAlertApi {
    @GET("transactions")
    suspend fun getTransactions(
        @Query("api_key") apiKey: String,
        @Query("min_value") minValue: Int = 1000000,
        @Query("limit") limit: Int = 20
    ): WhaleAlertResponse
}

data class WhaleAlertResponse(
    val result: String,
    val count: Int,
    val transactions: List<WhaleTransaction>
)

data class WhaleTransaction(
    val blockchain: String,
    val symbol: String,
    val transaction_type: String,
    val hash: String,
    val from: WhaleEntity,
    val to: WhaleTransactionEntity,
    val timestamp: Long,
    val amount: Double,
    val amount_usd: Double
)

data class WhaleEntity(
    val address: String?,
    val owner: String?,
    val owner_type: String?
)

data class WhaleTransactionEntity(
    val address: String?,
    val owner: String?,
    val owner_type: String?
)