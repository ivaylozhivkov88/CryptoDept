package com.cryptodept.data.repository

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import android.util.Log
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
        val activeAlerts = alertDao.getActiveAlerts()
        Log.d("CryptoDept_Alerts", "Checking ${activeAlerts.size} alerts for $coinId. Current: $currentPrice")
        
        activeAlerts.filter { it.coinId == coinId }.forEach { alert ->
            val triggered = when (alert.direction) {
                AlertDirection.ABOVE -> currentPrice >= alert.targetPrice
                AlertDirection.BELOW -> currentPrice <= alert.targetPrice
            }

            if (triggered) {
                Log.i("CryptoDept_Alerts", "TRIGGERED: $coinId reached ${alert.targetPrice}")
                alertDao.markAsTriggered(alert.id)
                showNotification(alert, currentPrice)
            }
        }
    }

    private fun showNotification(alert: AlertEntity, currentPrice: Double) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = CryptoPriceForegroundService.CHANNEL_ALERTS_ID

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.alert_notification_title, alert.coinSymbol))
            .setContentText(context.getString(R.string.alert_notification_content, alert.coinSymbol, currentPrice.toString()))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(alert.id, notification)
    }
}