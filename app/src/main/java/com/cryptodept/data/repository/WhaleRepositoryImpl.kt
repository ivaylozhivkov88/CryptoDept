package com.cryptodept.data.repository

import com.cryptodept.data.api.EtherscanWhaleClient
import com.cryptodept.data.api.HeliusWhaleClient
import com.cryptodept.data.api.MempoolWhaleClient
import com.cryptodept.domain.model.Blockchain
import com.cryptodept.domain.model.WhaleTransaction
import com.cryptodept.domain.repository.WhaleRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhaleRepositoryImpl
    @Inject
    constructor(
        private val etherscanClient: EtherscanWhaleClient,
        private val heliusClient: HeliusWhaleClient,
        private val btcClient: MempoolWhaleClient,
        private val firebaseDataSource: com.cryptodept.data.remote.source.FirebaseRemoteDataSource,
    ) : WhaleRepository {
        private val _transactions = MutableStateFlow<List<WhaleTransaction>>(emptyList())

        override fun getWhaleTransactions(): Flow<List<WhaleTransaction>> = 
            merge(
                _transactions.asStateFlow(),
                firebaseDataSource.getTerminalState().map { state ->
                    state?.whaleAlerts?.map { alert ->
                        WhaleTransaction(
                            id = alert.timestamp.toString(),
                            blockchain = when {
                                alert.explorerUrl.contains("etherscan") -> Blockchain.ETHEREUM
                                alert.explorerUrl.contains("solscan") || alert.explorerUrl.contains("helius") -> Blockchain.SOLANA
                                else -> Blockchain.BITCOIN
                            },
                            amount = 0.0, // We usually care about USD more
                            amountUsd = alert.amountUsd,
                            symbol = alert.asset,
                            fromAddress = "Unknown",
                            toAddress = "Unknown",
                            timestamp = alert.timestamp,
                            transactionHash = alert.explorerUrl.split("/").last()
                        )
                    } ?: emptyList()
                }
            ).map { it.sortedByDescending { tx -> tx.timestamp }.distinctBy { tx -> tx.transactionHash } }

        override suspend fun refreshWhaleTransactions(): Result<Unit> =
            coroutineScope {
                try {
                    val ethTxsDeferred = async { etherscanClient.fetchWhaleTransactions() }
                    val solTxsDeferred = async { heliusClient.fetchWhaleTransactions() }
                    val btcTxsDeferred = async { btcClient.fetchWhaleTransactions() }

                    val allTxs =
                        (ethTxsDeferred.await() + solTxsDeferred.await() + btcTxsDeferred.await())
                            .sortedByDescending { it.timestamp }

                    _transactions.value = allTxs
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
    }
