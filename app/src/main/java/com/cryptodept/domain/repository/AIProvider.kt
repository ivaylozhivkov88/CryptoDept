package com.cryptodept.domain.repository

import com.cryptodept.domain.model.TradeJournal
import kotlinx.coroutines.flow.Flow

interface AIProvider {
    suspend fun sendMessage(prompt: String): Flow<String>

    suspend fun analyzeJournal(
        trades: List<TradeJournal>,
        currentRiskScore: Int,
        currentMarket: String,
    ): Flow<String>
}
