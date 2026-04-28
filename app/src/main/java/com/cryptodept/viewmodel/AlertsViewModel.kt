// STEP 9: AlertsViewModel for managing price alerts
// Created: 2024-05-22
// Dependencies: GetAlertsUseCase, AddAlertUseCase
// Used by: AlertsScreen

package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.model.Alert
import com.cryptodept.domain.usecase.AddAlertUseCase
import com.cryptodept.domain.usecase.GetAlertsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val getAlertsUseCase: GetAlertsUseCase,
    private val addAlertUseCase: AddAlertUseCase
) : ViewModel() {

    private val _alerts = MutableStateFlow<List<Alert>>(emptyList())
    val alerts: StateFlow<List<Alert>> = _alerts.asStateFlow()

    init {
        viewModelScope.launch {
            getAlertsUseCase().collect {
                _alerts.value = it
            }
        }
    }

    fun addAlert(coinId: String, coinSymbol: String, targetPrice: Double, direction: com.cryptodept.domain.model.AlertDirection) {
        viewModelScope.launch {
            val alert = Alert(
                coinId = coinId,
                coinSymbol = coinSymbol,
                targetPrice = targetPrice,
                direction = direction,
                isActive = true
            )
            addAlertUseCase(alert)
        }
    }
}
