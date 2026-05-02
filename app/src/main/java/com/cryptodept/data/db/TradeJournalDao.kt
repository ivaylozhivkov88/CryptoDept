package com.cryptodept.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeJournalDao {
    @Query("SELECT * FROM trade_journal ORDER BY entryTime DESC")
    fun getAllTrades(): Flow<List<TradeJournalEntity>>

    @Query("SELECT * FROM trade_journal WHERE status = 'OPEN'")
    fun getOpenTrades(): Flow<List<TradeJournalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: TradeJournalEntity)

    @Update
    suspend fun updateTrade(trade: TradeJournalEntity)

    @Delete
    suspend fun deleteTrade(trade: TradeJournalEntity)

    @Query("SELECT AVG(pnlPercent) FROM trade_journal WHERE status != 'OPEN'")
    suspend fun getAveragePnL(): Double?

    @Query("SELECT COUNT(*) FROM trade_journal WHERE status = 'CLOSED_WIN'")
    suspend fun getWinCount(): Int

    @Query("SELECT COUNT(*) FROM trade_journal WHERE status IN ('CLOSED_WIN', 'CLOSED_LOSS')")
    suspend fun getTotalClosedCount(): Int

    @Query("SELECT AVG(riskRewardActual) FROM trade_journal WHERE status != 'OPEN' AND riskRewardActual IS NOT NULL")
    suspend fun getAverageRR(): Double?

    @Query("SELECT * FROM trade_journal WHERE entryTime > :since ORDER BY entryTime DESC")
    suspend fun getAllTradesSince(since: Long): List<TradeJournalEntity>

    @Query("SELECT positionSizeUsd FROM trade_journal ORDER BY entryTime DESC LIMIT 20")
    suspend fun getRecentSizes(): List<Double>
}
