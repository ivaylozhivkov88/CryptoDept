package com.cryptodept.data.api

import com.cryptodept.domain.model.Blockchain
import com.cryptodept.domain.model.WhaleTransaction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MempoolWhaleClient
    @Inject
    constructor(
        private val mempoolApi: MempoolSpaceApi,
    ) {
        suspend fun fetchWhaleTransactions(): List<WhaleTransaction> =
            try {
                val recentTxs = mempoolApi.getRecentTransactions()
                recentTxs
                    .filter { it.value > 5_000_000_000L } // > 50 BTC (BTC has 8 decimals)
                    .map { it.toDomain() }
            } catch (e: Exception) {
                emptyList()
            }

        private fun MempoolTxDTO.toDomain(): WhaleTransaction {
            val btcValue = value.toDouble() / 1e8
            return WhaleTransaction(
                id = txid,
                blockchain = Blockchain.BITCOIN,
                amount = btcValue,
                amountUsd = btcValue * 65000, // Approx BTC price
                symbol = "BTC",
                fromAddress = "Multiple Inputs",
                toAddress = "Check Hash",
                timestamp = System.currentTimeMillis(), // Mempool recent doesn't give timestamp directly in this DTO easily
                transactionHash = txid,
            )
        }
    }
