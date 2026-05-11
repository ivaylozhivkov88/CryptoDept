package com.cryptodept.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CoinDao {
    @Query("SELECT * FROM coins WHERE id NOT IN ('tether', 'usd-coin', 'binance-usd', 'dai', 'true-usd', 'paxos-standard', 'frax', 'usdd', 'fdusd', 'pyusd', 'first-digital-usd', 'paypal-usd', 'ethena-usde')")
    fun getAllCoins(): Flow<List<CoinEntity>>

    @Query("SELECT * FROM coins WHERE isTracked = 1 AND id NOT IN ('tether', 'usd-coin', 'binance-usd', 'dai', 'true-usd', 'paxos-standard', 'frax', 'usdd', 'fdusd', 'pyusd', 'first-digital-usd', 'paypal-usd', 'ethena-usde')")
    fun getTrackedCoins(): Flow<List<CoinEntity>>

    @Query("SELECT * FROM coins WHERE id NOT IN ('tether', 'usd-coin', 'binance-usd', 'dai', 'true-usd', 'paxos-standard', 'frax', 'usdd', 'fdusd', 'pyusd', 'first-digital-usd', 'paypal-usd', 'ethena-usde') ORDER BY marketCap DESC LIMIT :limit")
    fun getTopCoins(limit: Int): Flow<List<CoinEntity>>

    @Query("SELECT COUNT(*) FROM coins WHERE isTracked = 1")
    suspend fun getTrackedCoinsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoins(coins: List<CoinEntity>)

    @Update
    suspend fun updateCoin(coin: CoinEntity)

    @Query("DELETE FROM coins WHERE symbol IN ('usdt', 'usdc', 'busd', 'dai', 'tusd', 'usdp', 'frax', 'usdd', 'fdusd', 'pyusd', 'usde')")
    suspend fun deleteStablecoins()

    @Query("SELECT * FROM coins WHERE id = :coinId")
    suspend fun getCoinById(coinId: String): CoinEntity?

    @Query("SELECT * FROM coins WHERE id = :coinId")
    fun getCoinPriceFlow(coinId: String): Flow<CoinEntity?>

    @Query(
        "UPDATE coins SET currentPrice = :newPrice, sourcesCount = :sourcesCount, maxDeviation = :deviation, lastUpdated = :timestamp WHERE id = :id",
    )
    suspend fun updatePrice(
        id: String,
        newPrice: Double,
        sourcesCount: Int,
        deviation: Double,
        timestamp: Long,
    )
}
