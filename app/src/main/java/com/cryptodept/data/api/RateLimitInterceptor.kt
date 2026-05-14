package com.cryptodept.data.api

import okhttp3.Interceptor
import okhttp3.Response
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap

class RateLimitInterceptor : Interceptor {
    private val requestTimestamps = ConcurrentHashMap<String, ArrayDeque<Long>>()

    // Rate limits (requests per minute)
    private val limits =
        mapOf(
            "api.coingecko.com" to 30,
            "api.coinglass.com" to 20,
            "api.etherscan.io" to 5,
            "www.alphavantage.co" to 5,
            "api.coinmarketcal.com" to 10,
        )

    override fun intercept(chain: Interceptor.Chain): Response {
        val host = chain.request().url.host
        val limit = limits[host] ?: return chain.proceed(chain.request())

        val window = 60_000L

        var waitMs = 0L
        val timestamps = requestTimestamps.getOrPut(host) { ArrayDeque() }
        synchronized(timestamps) {
            // Clear old timestamps
            while (timestamps.isNotEmpty() && System.currentTimeMillis() - (timestamps.peekFirst() ?: 0L) > window) {
                timestamps.removeFirst()
            }

            if (timestamps.size >= limit) {
                waitMs = window - (System.currentTimeMillis() - (timestamps.peekFirst() ?: 0L)) + 100
            } else {
                timestamps.addLast(System.currentTimeMillis())
            }
        }

        if (waitMs > 0) {
            // Sleep outside the synchronized block to avoid monitor contention
            Thread.sleep(waitMs)
            return intercept(chain) // Re-check limits after sleeping
        }

        val response = chain.proceed(chain.request())

        // Handle 429 with exponential backoff
        if (response.code == 429) {
            val retryAfter = response.header("Retry-After")?.toLongOrNull()?.times(1000) ?: 60_000L
            response.close()
            Thread.sleep(retryAfter)
            return intercept(chain)
        }

        return response
    }
}
