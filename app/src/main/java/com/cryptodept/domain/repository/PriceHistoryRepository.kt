package com.cryptodept.domain.repository

import com.cryptodept.data.db.PriceHistoryEntity
import kotlinx.coroutines.flow.Flow

interface PriceHistoryRepository {
    fun getPriceHistory(coinId: String): Flow<List<PriceHistoryEntity>>
    suspend fun saveDailyPrice(coinId: String, price: Double, volume: Double)
    suspend fun getPriceAtTimestamp(coinId: String, timestamp: Long): Double?
    suspend fun cleanup(daysToKeep: Int)
}
