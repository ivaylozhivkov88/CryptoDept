package com.cryptodept.util

import android.content.Context
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorMessageMapper
    @Inject
    constructor(
        @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    ) {
        fun map(throwable: Throwable): String =
            when (throwable) {
                is UnknownHostException -> "NO INTERNET CONNECTION"
                is SocketTimeoutException -> "CONNECTION TIMEOUT"
                is IOException -> "NETWORK ERROR"
                is HttpException -> {
                    when (throwable.code()) {
                        429 -> "RATE LIMIT EXCEEDED. TRY LATER."
                        404 -> "RESOURCE NOT FOUND"
                        500 -> "SERVER ERROR"
                        else -> "API ERROR (${throwable.code()})"
                    }
                }
                else -> throwable.message ?: "UNKNOWN ERROR OCCURRED"
            }

        /**
         * Map error for localized support if needed in the future.
         * Currently standardizing on English for the ELITE look.
         */
        fun mapElite(throwable: Throwable): String {
            val base = map(throwable)
            return ">>> ERROR: $base"
        }
    }
