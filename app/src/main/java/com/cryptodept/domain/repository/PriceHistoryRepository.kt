package com.cryptodept.domain.repository

import com.cryptodept.data.db.PriceHistoryEntity
import com.cryptodept.domain.model.OHLCData
import kotlinx.coroutines.flow.Flow

interface PriceHistoryRepository {
    fun getPriceHistory(coinId: String): Flow<List<PriceHistoryEntity>>
    suspend fun saveDailyPrice(coinId: String, price: Double, volume: Double)
    suspend fun saveOHLCData(coinId: String, data: List<OHLCData>)
    suspend fun getOHLCData(coinId: String, days: Int): List<OHLCData>
    suspend fun getPriceAtTimestamp(coinId: String, timestamp: Long): Double?
    suspend fun cleanup(daysToKeep: Int)
}
