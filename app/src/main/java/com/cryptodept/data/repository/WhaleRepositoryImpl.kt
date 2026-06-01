package com.cryptodept.data.repository

import com.cryptodept.domain.model.Blockchain
import com.cryptodept.domain.model.WhaleTransaction
import com.cryptodept.domain.repository.WhaleRepository
import com.cryptodept.data.remote.source.FirebaseRemoteDataSource
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhaleRepositoryImpl @Inject constructor(
    private val firebaseDataSource: FirebaseRemoteDataSource,
) : WhaleRepository {

    override fun getWhaleTransactions(): Flow<List<WhaleTransaction>> = 
        firebaseDataSource.getWhaleAlerts().map { alerts ->
            alerts.map { alert ->
                WhaleTransaction(
                    id = alert.timestamp.toString(),
                    blockchain = when {
                        alert.explorerUrl.contains("etherscan") -> Blockchain.ETHEREUM
                        alert.explorerUrl.contains("solscan") || alert.explorerUrl.contains("helius") -> Blockchain.SOLANA
                        else -> Blockchain.BITCOIN
                    },
                    amount = 0.0,
                    amountUsd = alert.amountUsd,
                    symbol = alert.asset,
                    fromAddress = "Unknown",
                    toAddress = "Unknown",
                    timestamp = alert.timestamp,
                    transactionHash = alert.explorerUrl.split("/").last()
                )
            }
        }.map { it.sortedByDescending { tx -> tx.timestamp } }

    override suspend fun refreshWhaleTransactions(): Result<Unit> {
        // --- PHASE O OPTIMIZATION: CLOUD-ONLY ---
        // We no longer make heavy API calls from the device.
        // Data is automatically harvested by Firebase Functions.
        return Result.success(Unit)
    }
}
