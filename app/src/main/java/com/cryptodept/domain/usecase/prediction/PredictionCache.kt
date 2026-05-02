package com.cryptodept.domain.usecase.prediction

import com.cryptodept.domain.model.PricePrediction
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

@Singleton
class PredictionCache @Inject constructor() {

    data class CacheEntry(
        val result: PricePrediction,
        val cachedAt: Long = System.currentTimeMillis()
    )

    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val TTL_MS = 5 * 60 * 1000L  // 5 минути

    fun get(coinId: String, timeframe: String): PricePrediction? {
        val key = "$coinId:$timeframe"
        val entry = cache[key] ?: return null
        if (System.currentTimeMillis() - entry.cachedAt > TTL_MS) {
            cache.remove(key)
            return null
        }
        return entry.result
    }

    fun put(coinId: String, timeframe: String, result: PricePrediction) {
        cache["$coinId:$timeframe"] = CacheEntry(result)
    }

    fun invalidate(coinId: String) {
        cache.keys.filter { it.startsWith("$coinId:") }.forEach { cache.remove(it) }
    }

    fun invalidateAll() = cache.clear()

    fun getStats(): String = "Cache: ${cache.size} entries, TTL: ${TTL_MS/1000}s"
}

