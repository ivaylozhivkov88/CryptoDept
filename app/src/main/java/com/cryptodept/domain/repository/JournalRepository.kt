package com.cryptodept.domain.repository

import com.cryptodept.domain.model.JournalStats
import com.cryptodept.domain.model.TradeJournal
import kotlinx.coroutines.flow.Flow

interface JournalRepository {
    fun getAllTrades(): Flow<List<TradeJournal>>

    fun getOpenTrades(): Flow<List<TradeJournal>>

    suspend fun addTrade(trade: TradeJournal): Result<Unit>

    suspend fun updateTrade(trade: TradeJournal): Result<Unit>

    suspend fun deleteTrade(trade: TradeJournal): Result<Unit>

    suspend fun getStats(): Result<JournalStats>
}
