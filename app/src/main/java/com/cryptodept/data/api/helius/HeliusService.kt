package com.cryptodept.data.api.helius

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface HeliusService {
    @GET("v0/addresses/{address}/transactions")
    suspend fun getAddressTransactions(
        @Path("address") address: String,
        @Query("api-key") apiKey: String,
        @Query("limit") limit: Int = 50,
    ): List<HeliusTxDto>
}

data class HeliusTxDto(
    val signature: String,
    val timestamp: Long,
    val nativeTransfers: List<HeliusNativeTransfer>?,
)

data class HeliusNativeTransfer(
    val fromUserAccount: String?,
    val toUserAccount: String?,
    val amount: Long,
)
