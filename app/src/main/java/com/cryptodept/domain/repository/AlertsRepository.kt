package com.cryptodept.domain.repository

import com.cryptodept.domain.model.Alert
import kotlinx.coroutines.flow.Flow

interface AlertsRepository {
    fun getAlerts(): Flow<List<Alert>>
    suspend fun insertAlert(alert: Alert)
    suspend fun deleteAlert(alert: Alert)
    suspend fun updateAlert(alert: Alert)
    suspend fun checkAlerts(coinId: String, currentPrice: Double)
}