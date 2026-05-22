package com.cryptodept.data.repository

import android.util.Log
import com.cryptodept.data.db.AlertDao
import com.cryptodept.data.db.AlertEntity
import com.cryptodept.domain.model.Alert
import com.cryptodept.domain.model.AlertDirection
import com.cryptodept.domain.model.CompositeAlert
import com.cryptodept.domain.repository.AlertsRepository
import com.cryptodept.service.AlertNotificationService
import com.cryptodept.util.HapticService
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertsRepositoryImpl
    @Inject
    constructor(
        private val alertDao: AlertDao,
        private val alertNotificationService: AlertNotificationService,
        private val hapticService: HapticService,
        private val analytics: com.cryptodept.util.AnalyticsService,
        private val gson: Gson,
    ) : AlertsRepository {
        override fun getAlerts(): Flow<List<Alert>> =
            alertDao.getAllAlerts().map { entities ->
                entities.map { it.toDomain() }
            }

        override fun getCompositeAlerts(): Flow<List<CompositeAlert>> =
            alertDao.getAllAlerts().map { entities ->
                entities.map { it.toCompositeDomain(gson) }
            }

        override suspend fun insertAlert(alert: Alert) {
            analytics.logAlertCreated(alert.coinSymbol, alert.direction.name)
            alertDao.insertAlert(AlertEntity.fromDomain(alert))
        }

        override suspend fun insertCompositeAlert(alert: CompositeAlert) {
            analytics.logAlertCreated(alert.coinSymbol, "COMPOSITE")
            alertDao.insertAlert(AlertEntity.fromComposite(alert, gson))
        }

        override suspend fun deleteAlert(alertId: Int) {
            // Simplified delete
            val dummy =
                AlertEntity(
                    id = alertId,
                    coinId = "",
                    coinSymbol = "",
                    targetPrice = 0.0,
                    direction = AlertDirection.ABOVE,
                    isActive = false,
                    isTriggered = false,
                    createdAt = 0L,
                )
            alertDao.deleteAlert(dummy)
        }

        override suspend fun updateAlert(alert: Alert) {
            alertDao.updateAlert(AlertEntity.fromDomain(alert))
        }

        override suspend fun updateCompositeAlert(alert: CompositeAlert) {
            alertDao.updateAlert(AlertEntity.fromComposite(alert, gson))
        }

        override suspend fun checkAlerts(
            coinId: String,
            currentPrice: Double,
        ) {
            val activeAlerts = alertDao.getActiveAlerts()

            activeAlerts.filter { it.coinId == coinId }.forEach { alert ->
                if (alert.conditionsJson == null) {
                    // Legacy simple price check
                    val triggered =
                        when (alert.direction) {
                            AlertDirection.ABOVE -> currentPrice >= alert.targetPrice
                            AlertDirection.BELOW -> currentPrice <= alert.targetPrice
                        }

                    if (triggered) {
                        Log.i("AlertsRepository", "Triggered: ${alert.coinSymbol} at $currentPrice")
                        alertDao.markAsTriggered(alert.id)
                        hapticService.priceAlert()
                        alertNotificationService.showPriceAlert(alert, currentPrice)
                    }
                } else {
                    // NEW: Composite alerts check would need a full technical snapshot
                    // This is typically handled by a worker or a centralized tracker
                }
            }
        }
    }
