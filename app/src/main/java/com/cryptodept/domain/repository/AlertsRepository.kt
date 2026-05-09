package com.cryptodept.domain.repository

import com.cryptodept.domain.model.Alert
import com.cryptodept.domain.model.CompositeAlert
import kotlinx.coroutines.flow.Flow

interface AlertsRepository {
    fun getAlerts(): Flow<List<Alert>>

    fun getCompositeAlerts(): Flow<List<CompositeAlert>>

    suspend fun insertAlert(alert: Alert)

    suspend fun insertCompositeAlert(alert: CompositeAlert)

    suspend fun deleteAlert(alertId: Int)

    suspend fun updateAlert(alert: Alert)

    suspend fun updateCompositeAlert(alert: CompositeAlert)

    suspend fun checkAlerts(
        coinId: String,
        currentPrice: Double,
    )
}
