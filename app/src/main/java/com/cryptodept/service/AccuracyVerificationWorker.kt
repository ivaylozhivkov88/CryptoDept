package com.cryptodept.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.cryptodept.domain.usecase.PredictionAccuracyTracker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class AccuracyVerificationWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val accuracyTracker: PredictionAccuracyTracker,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result =
            try {
                accuracyTracker.verifyPredictions()
                Result.success()
            } catch (e: Exception) {
                Result.retry()
            }

        companion object {
            private const val WORK_NAME = "accuracy_verification_worker"

            fun schedule(context: Context) {
                val constraints =
                    Constraints
                        .Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build()

                val request =
                    PeriodicWorkRequestBuilder<AccuracyVerificationWorker>(6, TimeUnit.HOURS)
                        .setConstraints(constraints)
                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
                        .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request,
                )
            }
        }
    }
