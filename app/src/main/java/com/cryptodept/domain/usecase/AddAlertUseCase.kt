package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.Alert
import com.cryptodept.domain.repository.AlertsRepository
import javax.inject.Inject

class AddAlertUseCase @Inject constructor(
    private val repository: AlertsRepository
) {
    suspend operator fun invoke(alert: Alert) {
        repository.insertAlert(alert)
    }
}