package com.cryptodept.data.repository

import com.cryptodept.data.db.TradeJournalDao
import com.cryptodept.data.db.TradeJournalEntity
import com.cryptodept.domain.model.JournalStats
import com.cryptodept.domain.model.TradeDirection
import com.cryptodept.domain.model.TradeJournal
import com.cryptodept.domain.model.TradeStatus
import com.cryptodept.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JournalRepositoryImpl @Inject constructor(
    private val dao: TradeJournalDao
) : JournalRepository {

    override fun getAllTrades(): Flow<List<TradeJournal>> {
        return dao.getAllTrades().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getOpenTrades(): Flow<List<TradeJournal>> {
        return dao.getOpenTrades().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addTrade(trade: TradeJournal): Result<Unit> {
        return try {
            dao.insertTrade(trade.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTrade(trade: TradeJournal): Result<Unit> {
        return try {
            dao.updateTrade(trade.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTrade(trade: TradeJournal): Result<Unit> {
        return try {
            dao.deleteTrade(trade.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getStats(): Result<JournalStats> {
        return try {
            val avgPnl = dao.getAveragePnL() ?: 0.0
            val winCount = dao.getWinCount()
            val totalCount = dao.getTotalClosedCount()
            val avgRR = dao.getAverageRR() ?: 0.0
            
            val winRate = if (totalCount > 0) (winCount.toDouble() / totalCount) * 100 else 0.0
            
            Result.success(JournalStats(avgPnl, winRate, totalCount, avgRR))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun TradeJournalEntity.toDomain() = TradeJournal(
        id = id,
        coinId = coinId,
        symbol = symbol,
        direction = TradeDirection.valueOf(direction),
        entryPrice = entryPrice,
        exitPrice = exitPrice,
        quantity = quantity,
        entryTime = entryTime,
        exitTime = exitTime,
        riskPercent = riskPercent,
        stopLoss = stopLoss,
        takeProfit = takeProfit,
        notes = notes,
        status = TradeStatus.valueOf(status),
        pnlUsd = pnlUsd,
        pnlPercent = pnlPercent,
        riskRewardActual = riskRewardActual,
        positionSizeUsd = positionSizeUsd,
        marketConditions = marketConditions
    )

    private fun TradeJournal.toEntity() = TradeJournalEntity(
        id = id,
        coinId = coinId,
        symbol = symbol,
        direction = direction.name,
        entryPrice = entryPrice,
        exitPrice = exitPrice,
        quantity = quantity,
        entryTime = entryTime,
        exitTime = exitTime,
        riskPercent = riskPercent,
        stopLoss = stopLoss,
        takeProfit = takeProfit,
        notes = notes,
        status = status.name,
        pnlUsd = pnlUsd,
        pnlPercent = pnlPercent,
        riskRewardActual = riskRewardActual,
        positionSizeUsd = positionSizeUsd,
        marketConditions = marketConditions
    )
}
