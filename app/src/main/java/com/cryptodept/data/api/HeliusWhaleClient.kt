package com.cryptodept.data.api

import com.cryptodept.BuildConfig
import com.cryptodept.domain.model.Blockchain
import com.cryptodept.domain.model.WhaleTransaction
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HeliusWhaleClient
    @Inject
    constructor(
        private val heliusApi: HeliusApi,
    ) {
        // Top Solana Whale addresses
        private val whaleAddresses =
            listOf(
                "5tzCnkKdz2EB9A17GKG3kKx5m9Fm8FvB84L9B3H5N9N9", // Associated with large liquidity
                "2wmVCRW9unYvXvS89T44YVp16LhF4pGfS7s5N1mU8N4L", // Jump Trading
                "9W5uF3H6vTmq9YrkW7F4L4nS7m2Fv5C5C7N4H5H9N4H5", // Wintermute
            )

        suspend fun fetchWhaleTransactions(): List<WhaleTransaction> {
            val apiKey = BuildConfig.HELIUS_API_KEY
            if (apiKey.isBlank()) return emptyList()

            val allTransactions = mutableListOf<WhaleTransaction>()

            whaleAddresses.forEach { address ->
                try {
                    val txs = heliusApi.getAddressTransactions(address, apiKey)
                    txs.forEach { dto ->
                        // Filter for large transfers
                        val largeNative = dto.nativeTransfers?.filter { it.amount > 1_000_000_000_000L } // > 1000 SOL (SOL has 9 decimals)
                        val largeToken = dto.tokenTransfers?.filter { it.amount.toDoubleOrNull() ?: 0.0 > 100_000_000_000.0 } // Placeholder for large token move

                        if (!largeNative.isNullOrEmpty() || !largeToken.isNullOrEmpty()) {
                            allTransactions.add(dto.toDomain())
                        }
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }
            return allTransactions
        }

        private fun HeliusTxDTO.toDomain(): WhaleTransaction {
            val amount = (nativeTransfers?.firstOrNull()?.amount?.toDouble() ?: 0.0) / 1e9
            return WhaleTransaction(
                id = signature,
                blockchain = Blockchain.SOLANA,
                amount = amount,
                amountUsd = amount * 150, // Approx SOL price
                symbol = "SOL",
                fromAddress = nativeTransfers?.firstOrNull()?.fromUserAccount ?: "Unknown",
                toAddress = nativeTransfers?.firstOrNull()?.toUserAccount ?: "Unknown",
                timestamp = timestamp * 1000,
                transactionHash = signature,
            )
        }
    }
