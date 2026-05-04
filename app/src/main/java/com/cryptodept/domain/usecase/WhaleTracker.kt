package com.cryptodept.domain.usecase

import com.cryptodept.BuildConfig
import com.cryptodept.data.api.WhaleAlertApi
import com.cryptodept.data.api.WhaleTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhaleTracker @Inject constructor(
    private val whaleAlertApi: WhaleAlertApi
) {
    suspend fun getRecentWhaleMoves(): List<WhaleTransaction> = withContext(Dispatchers.IO) {
        try {
            // Using a placeholder or BuildConfig if available. 
            // For now, if key is empty, this will fail or return empty.
            val apiKey = BuildConfig.WHALE_ALERT_API_KEY
            val response = whaleAlertApi.getTransactions(apiKey = apiKey, minValue = 1000000)
            if (response.result == "success") {
                response.transactions
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}