package com.cryptodept.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cryptodept.domain.usecase.whale.AggregateWhaleActivityUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import android.util.Log

@HiltWorker
class WhaleMonitoringWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val aggregator: AggregateWhaleActivityUseCase,
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val transactions = aggregator.execute(minUsd = 1_000_000.0)
            Log.d("WhaleWorker", "Scanned ${transactions.size} large transactions.")
            // For now just log, later we can add notifications
            Result.success()
        } catch (e: Exception) {
            Log.e("WhaleWorker", "Failed to scan whales", e)
            Result.retry()
        }
    }
}
