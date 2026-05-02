package com.cryptodept.domain.repository

import com.cryptodept.domain.model.CoinDetail
import com.cryptodept.domain.model.CoinPrice
import com.cryptodept.domain.model.GlobalMarketData
import com.cryptodept.domain.model.OHLCData
import kotlinx.coroutines.flow.Flow

interface CryptoRepository {
    fun getTrackedCoinPrices(): Flow<List<CoinPrice>>
    suspend fun refreshPrices(): Result<Unit>
    fun getCoinPrice(coinId: String): Flow<CoinPrice?>
    suspend fun getOHLCData(coinId: String, days: Int): List<OHLCData>
    suspend fun getCurrentPrice(coinId: String): Double
    suspend fun getCachedPrice(coinId: String): Double
    suspend fun getCachedChange24h(coinId: String): Double
    suspend fun getCoinDetail(coinId: String): Result<CoinDetail>
    suspend fun getGlobalMarketData(): Result<GlobalMarketData>
    suspend fun getPriceAtTimestamp(coinId: String, timestamp: Long): Double
    suspend fun toggleTracking(coinId: String): Result<Unit>
}
