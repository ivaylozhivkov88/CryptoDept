package com.cryptodept.domain.usecase.whale

import com.cryptodept.domain.model.TransactionType
import com.cryptodept.domain.model.WhaleSignal
import javax.inject.Inject

/**
 * Computes a high-level market signal based on aggregate whale capital flow.
 */
class GetWhaleInsightUseCase @Inject constructor(
    private val aggregateWhaleActivityUseCase: AggregateWhaleActivityUseCase,
) {
    suspend fun execute(): WhaleSignal {
        val transactions = try {
            aggregateWhaleActivityUseCase.execute(maxPerChain = 20)
        } catch (e: Exception) {
            return WhaleSignal.NEUTRAL
        }
        
        if (transactions.isEmpty()) return WhaleSignal.NEUTRAL
        
        val now = System.currentTimeMillis()
        val last24h = now - (24 * 60 * 60 * 1000L)
        
        val recentTxs = transactions.filter { it.timestamp >= last24h }
        if (recentTxs.isEmpty()) return WhaleSignal.NEUTRAL

        val totalDeposits = recentTxs
            .filter { it.transactionType == TransactionType.EXCHANGE_DEPOSIT }
            .sumOf { it.amountUsd }
        
        val totalWithdrawals = recentTxs
            .filter { it.transactionType == TransactionType.EXCHANGE_WITHDRAWAL }
            .sumOf { it.amountUsd }
        
        val ratio = if (totalDeposits > 0) totalWithdrawals / totalDeposits else if (totalWithdrawals > 0) 2.0 else 1.0
        
        return when {
            ratio > 1.5 -> WhaleSignal.BULLISH_HEAVY
            ratio > 1.1 -> WhaleSignal.BULLISH
            ratio < 0.66 -> WhaleSignal.BEARISH_HEAVY
            ratio < 0.9 -> WhaleSignal.BEARISH
            else -> WhaleSignal.NEUTRAL
        }
    }
}
