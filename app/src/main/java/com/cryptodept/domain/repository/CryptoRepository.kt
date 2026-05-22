package com.cryptodept.domain.repository

import com.cryptodept.domain.model.*
import kotlinx.coroutines.flow.Flow

interface CryptoRepository {
    fun getTrackedCoinPrices(): Flow<List<CoinPrice>>

    fun getAllCoinPrices(): Flow<List<CoinPrice>>

    suspend fun refreshPrices(): CryptoResult<Unit>

    fun getCoinPrice(coinId: String): Flow<CoinPrice?>

    suspend fun getOHLCData(
        coinId: String,
        days: Int,
    ): List<OHLCData>

    suspend fun getCurrentPrice(coinId: String): Double

    suspend fun getCachedPrice(coinId: String): Double

    suspend fun getCachedChange24h(coinId: String): Double

    suspend fun getCoinDetail(coinId: String): CryptoResult<CoinDetail>

    suspend fun getGlobalMarketData(): CryptoResult<GlobalMarketData>

    fun getGlobalMarketDataFlow(): Flow<GlobalMarketData?>

    suspend fun getPriceAtTimestamp(
        coinId: String,
        timestamp: Long,
    ): Double

    suspend fun toggleTracking(coinId: String): CryptoResult<Unit>

    fun getNetworkHealth(): Flow<NetworkHealth?>

    suspend fun saveNetworkHealth(health: NetworkHealth)
}
