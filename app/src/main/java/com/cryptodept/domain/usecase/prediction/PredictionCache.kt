package com.cryptodept.domain.usecase.prediction

import com.cryptodept.domain.model.PricePrediction
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PredictionCache
    @Inject
    constructor() {
        data class CacheEntry(
            val result: PricePrediction,
            val cachedAt: Long = System.currentTimeMillis(),
        )

        private val cache = ConcurrentHashMap<String, CacheEntry>()
        private val TTL_MS = 15 * 60 * 1000L // 15 minutes (Change 3)
        private val MAX_SIZE = 10 // Max coins cached simultaneously

        fun get(
            coinId: String,
            timeframe: String,
        ): PricePrediction? {
            val key = "$coinId:$timeframe"
            val entry = cache[key] ?: return null
            if (System.currentTimeMillis() - entry.cachedAt > TTL_MS) {
                cache.remove(key)
                return null
            }
            return entry.result
        }

        fun put(
            coinId: String,
            timeframe: String,
            result: PricePrediction,
        ) {
            if (cache.size >= MAX_SIZE) {
                // Remove oldest (not strictly LRU with ConcurrentHashMap but good enough)
                val oldestKey = cache.keys.firstOrNull()
                oldestKey?.let { cache.remove(it) }
            }
            cache["$coinId:$timeframe"] = CacheEntry(result)
        }

        fun invalidate(coinId: String) {
            cache.keys.filter { it.startsWith("$coinId:") }.forEach { cache.remove(it) }
        }

        fun invalidateAll() = cache.clear()

        fun getStats(): String = "Cache: ${cache.size} entries, TTL: ${TTL_MS / 1000}s"
    }
