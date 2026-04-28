package com.cryptodept.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceHistoryDao {
    @Query("SELECT * FROM price_history WHERE coinId = :coinId ORDER BY timestamp DESC")
    fun getPriceHistory(coinId: String): Flow<List<PriceHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrice(price: PriceHistoryEntity)

    @Query("DELETE FROM price_history WHERE timestamp < :threshold")
    suspend fun deleteOldHistory(threshold: Long)
}