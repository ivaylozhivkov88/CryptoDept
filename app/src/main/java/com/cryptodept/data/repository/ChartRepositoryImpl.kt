package com.cryptodept.data.repository

import android.util.Log
import com.cryptodept.data.api.CoinGeckoApi
import com.cryptodept.domain.model.OHLCData
import com.cryptodept.domain.repository.ChartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChartRepositoryImpl @Inject constructor(
    private val api: CoinGeckoApi
) : ChartRepository {

    // In-memory cache: key = "coinId_days", value = OHLC list
    private val _cache = MutableStateFlow<Map<String, List<OHLCData>>>(emptyMap())

    // Timestamp на последния fetch за всеки ключ
    private val lastFetchTime = mutableMapOf<String, Long>()
    private val CACHE_TTL_MS = 5 * 60 * 1000L // 5 минути

    override fun getOHLCData(coinId: String, days: Int): Flow<List<OHLCData>> {
        val cacheKey = "${coinId}_${days}"
        return _cache.map { cache -> cache[cacheKey] ?: emptyList() }
    }

    override suspend fun refreshOHLCData(coinId: String, days: Int): Result<Unit> {
        val cacheKey = "${coinId}_${days}"

        // Не fetch-вай ако кешът е пресен (< 5 мин)
        val lastFetch = lastFetchTime[cacheKey] ?: 0L
        if (System.currentTimeMillis() - lastFetch < CACHE_TTL_MS) {
            val cached = _cache.value[cacheKey]
            if (!cached.isNullOrEmpty()) {
                Log.d("CryptoDept_CACHE", "💾 OHLC cache hit for $coinId ($days days)")
                return Result.success(Unit)
            }
        }

        return try {
            Log.d("CryptoDept_API", "🌐 Fetching OHLC for $coinId ($days days)...")
            val response = api.getCoinOHLC(id = coinId, days = days.toString())

            val data = response.map { item ->
                OHLCData(
                    timestamp = item[0].toLong(),
                    open = item[1],
                    high = item[2],
                    low = item[3],
                    close = item[4],
                    volume = 0.0
                )
            }

            if (data.isEmpty()) {
                Log.w("CryptoDept_API", "⚠ Empty OHLC response for $coinId")
                return Result.failure(Exception("Empty OHLC data for $coinId"))
            }

            // Обнови кеша
            val currentMap = _cache.value.toMutableMap()
            currentMap[cacheKey] = data
            _cache.value = currentMap
            lastFetchTime[cacheKey] = System.currentTimeMillis()

            Log.d("CryptoDept_API", "✅ OHLC loaded: ${data.size} candles for $coinId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("CryptoDept_API", "❌ OHLC fetch failed for $coinId: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Връща кешираните OHLC данни директно (без Flow).
     * Използва се от AnalysisViewModel за бърз достъп.
     */
    fun getCachedOHLC(coinId: String, days: Int): List<OHLCData> {
        val cacheKey = "${coinId}_${days}"
        return _cache.value[cacheKey] ?: emptyList()
    }
}