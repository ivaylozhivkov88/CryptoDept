package com.cryptodept.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.cryptodept.domain.model.AlertDirection
import com.cryptodept.domain.repository.AlertsRepository
import com.cryptodept.domain.repository.CryptoRepository
import com.cryptodept.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class AlertWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val alertRepository: AlertsRepository,
    private val cryptoRepository: CryptoRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            val activeAlerts = alertRepository.getAlerts().first().filter { it.isActive }
            if (activeAlerts.isEmpty()) return Result.success()

            activeAlerts.forEach { alert ->
                val currentPrice = cryptoRepository.getCurrentPrice(alert.coinId)
                if (currentPrice <= 0) return@forEach

                // Проверка дали цената е ударила таргета
                val isTriggered = if (alert.direction == AlertDirection.ABOVE) {
                    currentPrice >= alert.targetPrice
                } else {
                    currentPrice <= alert.targetPrice
                }

                if (isTriggered) {
                    notificationHelper.showPriceAlert(
                        alert.coinSymbol,
                        alert.targetPrice,
                        currentPrice
                    )
                    // Маркираме алармата като неактивна
                    alertRepository.updateAlert(alert.copy(isActive = false, isTriggered = true))
                }
            }
            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }

    companion object {
        private const val ALERT_WORK_NAME = "price_alert_worker"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<AlertWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                ALERT_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
