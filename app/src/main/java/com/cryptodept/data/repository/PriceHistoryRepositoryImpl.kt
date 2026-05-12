package com.cryptodept.data.repository

import com.cryptodept.data.db.PriceHistoryDao
import com.cryptodept.data.db.PriceHistoryEntity
import com.cryptodept.domain.repository.PriceHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PriceHistoryRepositoryImpl @Inject constructor(
    private val priceHistoryDao: PriceHistoryDao
) : PriceHistoryRepository {

    override fun getPriceHistory(coinId: String): Flow<List<PriceHistoryEntity>> {
        return priceHistoryDao.getPriceHistory(coinId)
    }

    override suspend fun saveDailyPrice(coinId: String, price: Double, volume: Double) {
        val now = System.currentTimeMillis()
        // Normalize to start of day (UTC)
        val startOfDay = now - (now % (24 * 60 * 60 * 1000))
        
        val entity = PriceHistoryEntity(
            coinId = coinId,
            timestamp = startOfDay,
            price = price,
            volume = volume
        )
        priceHistoryDao.insertPrice(entity)
    }

    override suspend fun getPriceAtTimestamp(coinId: String, timestamp: Long): Double? {
        return priceHistoryDao.getNearestPriceBeforeTimestamp(coinId, timestamp)
    }

    override suspend fun cleanup(daysToKeep: Int) {
        val threshold = System.currentTimeMillis() - (daysToKeep.toLong() * 24 * 60 * 60 * 1000)
        priceHistoryDao.deleteOldHistory(threshold)
    }
}
