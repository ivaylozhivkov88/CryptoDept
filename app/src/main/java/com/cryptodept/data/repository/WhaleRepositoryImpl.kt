package com.cryptodept.data.repository

import com.cryptodept.data.api.EtherscanWhaleClient
import com.cryptodept.data.api.HeliusWhaleClient
import com.cryptodept.data.api.MempoolWhaleClient
import com.cryptodept.domain.model.WhaleTransaction
import com.cryptodept.domain.repository.WhaleRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhaleRepositoryImpl
    @Inject
    constructor(
        private val etherscanClient: EtherscanWhaleClient,
        private val heliusClient: HeliusWhaleClient,
        private val btcClient: MempoolWhaleClient,
    ) : WhaleRepository {
        private val _transactions = MutableStateFlow<List<WhaleTransaction>>(emptyList())

        override fun getWhaleTransactions(): Flow<List<WhaleTransaction>> = _transactions.asStateFlow()

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
