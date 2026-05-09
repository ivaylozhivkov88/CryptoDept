package com.cryptodept.data.api

import retrofit2.http.GET
import retrofit2.http.Path

interface MempoolSpaceApi {
    @GET("mempool/recent")
    suspend fun getRecentTransactions(): List<MempoolTxDTO>

    @GET("tx/{txid}")
    suspend fun getTransaction(
        @Path("txid") txid: String,
    ): MempoolTxDetailDTO
}

data class MempoolTxDTO(
    val txid: String,
    val fee: Long,
    val vsize: Int,
    val value: Long,
)

data class MempoolTxDetailDTO(
    val txid: String,
    val version: Int,
    val locktime: Long,
    val vin: List<Vin>,
    val vout: List<Vout>,
    val status: TxStatus,
)

data class Vin(
    val txid: String,
    val vout: Int,
    val prevout: Vout?,
    val scriptsig: String,
    val sequence: Long,
)

data class Vout(
    val scriptpubkey: String,
    val scriptpubkey_address: String?,
    val value: Long,
)

data class TxStatus(
    val confirmed: Boolean,
    val block_height: Int?,
    val block_hash: String?,
    val block_time: Long?,
)
