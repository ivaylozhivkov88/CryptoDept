// STEP 9: AlertsViewModel for managing price alerts
// Created: 2024-05-22
// Dependencies: GetAlertsUseCase, AddAlertUseCase
// Used by: AlertsScreen

package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.model.Alert
import com.cryptodept.domain.tier.AccessTier
import com.cryptodept.domain.tier.TierAccessManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertsViewModel
    @Inject
    constructor(
        private val repository: com.cryptodept.domain.repository.AlertsRepository,
        private val tierAccessManager: TierAccessManager,
    ) : ViewModel() {
        companion object {
            const val FREE_TIER_ALERT_LIMIT = 3
        }

        private val _alerts = MutableStateFlow<List<Alert>>(emptyList())
        val alerts: StateFlow<List<Alert>> = _alerts.asStateFlow()

        private val _compositeAlerts = MutableStateFlow<List<com.cryptodept.domain.model.CompositeAlert>>(emptyList())
        val compositeAlerts: StateFlow<List<com.cryptodept.domain.model.CompositeAlert>> = _compositeAlerts.asStateFlow()

        init {
            loadAlerts()
        }

        private fun loadAlerts() {
            viewModelScope.launch(Dispatchers.IO) {
                repository
                    .getAlerts()
                    .catch { emit(emptyList()) }
                    .collect {
                        _alerts.value = it
                    }
            }
            viewModelScope.launch(Dispatchers.IO) {
                repository
                    .getCompositeAlerts()
                    .catch { emit(emptyList()) }
                    .collect {
                        _compositeAlerts.value = it
                    }
            }
        }

        fun addAlert(
            coinId: String,
            coinSymbol: String,
            targetPrice: Double,
            direction: com.cryptodept.domain.model.AlertDirection,
        ) {
            viewModelScope.launch(Dispatchers.IO) {
                val alert =
                    Alert(
                        coinId = coinId,
                        coinSymbol = coinSymbol,
                        targetPrice = targetPrice,
                        direction = direction,
                        isActive = true,
                    )
                repository.insertAlert(alert)
            }
        }

        fun addCompositeAlert(alert: com.cryptodept.domain.model.CompositeAlert) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.insertCompositeAlert(alert)
            }
        }

        fun deleteAlert(id: Int) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.deleteAlert(id)
            }
        }

        fun getCachedTier(): AccessTier = tierAccessManager.getCachedTier()

        fun canCreateNewAlert(): AlertCreationResult {
            val tier = tierAccessManager.getCachedTier()
            if (tier.canAccess(AccessTier.PRO)) return AlertCreationResult.Allowed

            val currentCount = _alerts.value.size
            return when {
                currentCount >= FREE_TIER_ALERT_LIMIT -> AlertCreationResult.LimitReached(
                    currentCount = currentCount,
                    limit = FREE_TIER_ALERT_LIMIT,
                    tierRequired = AccessTier.PRO,
                )
                currentCount == FREE_TIER_ALERT_LIMIT - 1 -> AlertCreationResult.ApproachingLimit(
                    currentCount = currentCount,
                    limit = FREE_TIER_ALERT_LIMIT,
                    remaining = 1,
                )
                else -> AlertCreationResult.Allowed
            }
        }
    }

sealed class AlertCreationResult {
    object Allowed : AlertCreationResult()
    data class ApproachingLimit(
        val currentCount: Int,
        val limit: Int,
        val remaining: Int,
    ) : AlertCreationResult()
    data class LimitReached(
        val currentCount: Int,
        val limit: Int,
        val tierRequired: AccessTier,
    ) : AlertCreationResult()
}
