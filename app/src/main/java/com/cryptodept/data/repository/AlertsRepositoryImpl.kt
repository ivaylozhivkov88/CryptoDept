package com.cryptodept.data.repository

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import com.cryptodept.R
import com.cryptodept.data.db.AlertDao
import com.cryptodept.data.db.AlertEntity
import com.cryptodept.domain.model.Alert
import com.cryptodept.domain.model.AlertDirection
import com.cryptodept.domain.repository.AlertsRepository
import com.cryptodept.service.CryptoPriceForegroundService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertsRepositoryImpl @Inject constructor(
    private val alertDao: AlertDao,
    @ApplicationContext private val context: Context
) : AlertsRepository {

    override fun getAlerts(): Flow<List<Alert>> {
        return alertDao.getAllAlerts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertAlert(alert: Alert) {
        alertDao.insertAlert(AlertEntity.fromDomain(alert))
    }

    override suspend fun deleteAlert(alert: Alert) {
        alertDao.deleteAlert(AlertEntity.fromDomain(alert))
    }

    override suspend fun updateAlert(alert: Alert) {
        alertDao.updateAlert(AlertEntity.fromDomain(alert))
    }

    override suspend fun checkAlerts(coinId: String, currentPrice: Double) {
        // Вземаме само активните алерти от базата
        val activeAlerts = alertDao.getActiveAlerts()

        activeAlerts.filter { it.coinId == coinId }.forEach { alert ->
            val triggered = when (alert.direction) {
                AlertDirection.ABOVE -> currentPrice >= alert.targetPrice
                AlertDirection.BELOW -> currentPrice <= alert.targetPrice
            }

            if (triggered) {
                Log.i("AlertsRepository", "Triggered: ${alert.coinSymbol} at $currentPrice")
                // Маркираме в БД, че е задействан, за да не спами
                alertDao.markAsTriggered(alert.id)
                showNotification(alert, currentPrice)
            }
        }
    }

    private fun showNotification(alert: AlertEntity, currentPrice: Double) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Използваме ID-то от твоя сървиз. Увери се, че CHANNEL_ALERTS_ID съществува там.
        val channelId = "crypto_alerts_channel"

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Увери се, че този ресурс съществува
            .setContentTitle("Price Alert: ${alert.coinSymbol}")
            .setContentText("Target reached: $currentPrice")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(alert.id, notification)
    }
}