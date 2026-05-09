package com.cryptodept.data.api

import android.os.Bundle
import com.cryptodept.util.AnalyticsService
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class FirebaseAnalyticsInterceptor
    @Inject
    constructor(
        private val analyticsService: AnalyticsService,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val startTime = System.currentTimeMillis()

            return try {
                val response = chain.proceed(request)
                val duration = System.currentTimeMillis() - startTime

                if (!response.isSuccessful) {
                    val bundle =
                        Bundle().apply {
                            putString("url", request.url.toString().substringBefore("?"))
                            putInt("code", response.code)
                            putLong("duration_ms", duration)
                        }
                    analyticsService.logEvent("network_error", bundle)
                    analyticsService.log("Network error: ${request.url.toString().substringBefore("?")} - Code: ${response.code}")
                }

                response
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                val bundle =
                    Bundle().apply {
                        putString("url", request.url.toString().substringBefore("?"))
                        putString("exception", e.javaClass.simpleName)
                        putLong("duration_ms", duration)
                    }
                analyticsService.logEvent("network_failure", bundle)
                analyticsService.recordException(e, "Network failure: ${request.url.toString().substringBefore("?")}")
                throw e
            }
        }
    }
