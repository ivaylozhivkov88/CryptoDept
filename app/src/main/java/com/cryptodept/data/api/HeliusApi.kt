package com.cryptodept.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface HeliusApi {
    @GET("v0/addresses/{address}/transactions")
    suspend fun getAddressTransactions(
        @Path("address") address: String,
        @Query("api-key") apiKey: String,
    ): List<HeliusTxDTO>
}

data class HeliusTxDTO(
    val signature: String,
    val timestamp: Long,
    val type: String,
    val source: String,
    val fee: Long,
    val nativeTransfers: List<NativeTransfer>?,
    val tokenTransfers: List<TokenTransfer>?,
    val description: String?,
)

data class NativeTransfer(
    val fromUserAccount: String,
    val toUserAccount: String,
    val amount: Long,
)

data class TokenTransfer(
    val fromUserAccount: String,
    val toUserAccount: String,
    val amount: String,
    val tokenAddress: String,
    val symbol: String?,
)
