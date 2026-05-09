package com.cryptodept.data.api

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.io.InterruptedIOException
import kotlin.math.pow

class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val initialDelayMs: Long = 1000,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var response: Response? = null
        var tryCount = 0
        var lastException: Exception? = null

        while (tryCount <= maxRetries) {
            try {
                if (response != null) response.close()
                response = chain.proceed(chain.request())

                if (response.isSuccessful) return response

                val shouldRetry = response.code == 429 || response.code in 500..599
                if (!shouldRetry || tryCount >= maxRetries) return response
            } catch (e: IOException) {
                lastException = e
                if (tryCount >= maxRetries) throw e
            }

            tryCount++
            val retryAfterSeconds = response?.header("Retry-After")?.toLongOrNull()
            val backoffMs = (initialDelayMs * 2.0.pow((tryCount - 1).toDouble())).toLong()
            val waitTimeMs = (retryAfterSeconds?.times(1000) ?: backoffMs).coerceAtMost(30_000L)

            try {
                Thread.sleep(waitTimeMs)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                return response ?: throw lastException ?: InterruptedIOException()
            }
        }

        return response ?: throw lastException ?: IOException("Unknown retry error")
    }
}
