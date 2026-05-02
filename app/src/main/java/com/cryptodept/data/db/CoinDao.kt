package com.cryptodept.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CoinDao {
    @Query("SELECT * FROM coins")
    fun getAllCoins(): Flow<List<CoinEntity>>

    @Query("SELECT * FROM coins WHERE isTracked = 1")
    fun getTrackedCoins(): Flow<List<CoinEntity>>

    @Query("SELECT COUNT(*) FROM coins WHERE isTracked = 1")
    suspend fun getTrackedCoinsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoins(coins: List<CoinEntity>)

    @Update
    suspend fun updateCoin(coin: CoinEntity)

    @Query("SELECT * FROM coins WHERE id = :coinId")
    suspend fun getCoinById(coinId: String): CoinEntity?

    @Query("SELECT * FROM coins WHERE id = :coinId")
    fun getCoinPriceFlow(coinId: String): Flow<CoinEntity?>

    @Query("UPDATE coins SET currentPrice = :newPrice, sourcesCount = :sourcesCount, maxDeviation = :deviation, lastUpdated = :timestamp WHERE id = :id")
    suspend fun updatePrice(id: String, newPrice: Double, sourcesCount: Int, deviation: Double, timestamp: Long)
}