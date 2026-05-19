package com.cryptodept.data.api.mempool

import retrofit2.http.GET
import retrofit2.http.Path

interface MempoolService {
    @GET("address/{address}/txs")
    suspend fun getAddressTransactions(
        @Path("address") address: String,
    ): List<MempoolTxDto>
}

data class MempoolTxDto(
    val txid: String,
    val vin: List<MempoolVin>,
    val vout: List<MempoolVout>,
    val status: MempoolStatus,
)

data class MempoolVin(
    val prevout: MempoolPrevout?,
)

data class MempoolPrevout(
    val scriptpubkey_address: String?,
    val value: Long,
)

data class MempoolVout(
    val scriptpubkey_address: String?,
    val value: Long,
)

data class MempoolStatus(
    val block_time: Long?,
)
