package com.cryptodept.data.api

import okhttp3.Interceptor
import okhttp3.Response
import kotlin.math.pow

class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val initialDelayMs: Long = 1000
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var response = chain.proceed(chain.request())
        var tryCount = 0

        while (!response.isSuccessful && response.code == 429 && tryCount < maxRetries) {
            tryCount++

            val retryAfterSeconds = response.header("Retry-After")?.toLongOrNull()
            val backoffMs = (initialDelayMs * 2.0.pow((tryCount - 1).toDouble())).toLong()
            val waitTimeMs = (retryAfterSeconds?.times(1000) ?: backoffMs).coerceAtMost(30_000L)

            try {
                Thread.sleep(waitTimeMs)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                return response
            }

            response.close()
            response = chain.proceed(chain.request())
        }

        return response
    }
}