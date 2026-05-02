package com.cryptodept.data.api

import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.ConcurrentHashMap
import java.util.ArrayDeque

class RateLimitInterceptor : Interceptor {
    
    private val requestTimestamps = ConcurrentHashMap<String, ArrayDeque<Long>>()
    
    // Rate limits (requests per minute)
    private val limits = mapOf(
        "api.coingecko.com" to 30,
        "api.coinglass.com" to 20,
        "api.etherscan.io" to 5,
        "www.alphavantage.co" to 5,
        "api.coinmarketcal.com" to 10
    )
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val host = chain.request().url.host
        val limit = limits[host] ?: return chain.proceed(chain.request())
        
        val now = System.currentTimeMillis()
        val window = 60_000L
        
        val timestamps = requestTimestamps.getOrPut(host) { ArrayDeque() }
        synchronized(timestamps) {
            // Clear old timestamps
            while (timestamps.isNotEmpty() && now - (timestamps.peekFirst() ?: 0L) > window) {
                timestamps.removeFirst()
            }
            
            if (timestamps.size >= limit) {
                val waitMs = window - (now - (timestamps.peekFirst() ?: 0L)) + 100
                if (waitMs > 0) {
                    Thread.sleep(waitMs)
                }
            }
            
            timestamps.addLast(System.currentTimeMillis())
        }
        
        var response = chain.proceed(chain.request())
        
        // Handle 429 with exponential backoff
        if (response.code == 429) {
            val retryAfter = response.header("Retry-After")?.toLongOrNull()?.times(1000) ?: 60_000L
            response.close()
            Thread.sleep(retryAfter)
            response = chain.proceed(chain.request())
        }
        
        return response
    }
}
