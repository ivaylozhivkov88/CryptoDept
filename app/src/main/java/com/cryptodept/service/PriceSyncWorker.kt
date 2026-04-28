// STEP 8: WorkManager for background sync fallback
// Created: 2024-05-23
// Dependencies: Hilt, RefreshPricesUseCase
// Used by: Android System

package com.cryptodept.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cryptodept.domain.usecase.RefreshPricesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class PriceSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val refreshPricesUseCase: RefreshPricesUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val result = refreshPricesUseCase()
            if (result.isSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
