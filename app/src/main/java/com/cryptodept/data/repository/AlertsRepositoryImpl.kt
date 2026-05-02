package com.cryptodept.data.repository

import android.util.Log
import com.cryptodept.data.db.AlertDao
import com.cryptodept.data.db.AlertEntity
import com.cryptodept.domain.model.Alert
import com.cryptodept.domain.model.AlertDirection
import com.cryptodept.domain.repository.AlertsRepository
import com.cryptodept.service.AlertNotificationService
import com.cryptodept.util.HapticManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertsRepositoryImpl @Inject constructor(
    private val alertDao: AlertDao,
    private val alertNotificationService: AlertNotificationService,
    private val hapticManager: HapticManager,
    private val analytics: com.cryptodept.util.AnalyticsManager
) : AlertsRepository {

    override fun getAlerts(): Flow<List<Alert>> {
        return alertDao.getAllAlerts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertAlert(alert: Alert) {
        analytics.logAlertCreated(alert.coinSymbol, alert.direction.name)
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

        activeAlerts.filter { it.coinId == coinId }.forEach { alert ->
            val triggered = when (alert.direction) {
                AlertDirection.ABOVE -> currentPrice >= alert.targetPrice
                AlertDirection.BELOW -> currentPrice <= alert.targetPrice
            }

            if (triggered) {
                Log.i("AlertsRepository", "Triggered: ${alert.coinSymbol} at $currentPrice")
                alertDao.markAsTriggered(alert.id)
                hapticManager.alert()
                alertNotificationService.showPriceAlert(alert, currentPrice)
            }
        }
    }
}
