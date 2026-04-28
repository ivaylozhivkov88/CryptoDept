package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.Alert
import com.cryptodept.domain.repository.AlertsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAlertsUseCase @Inject constructor(
    private val repository: AlertsRepository
) {
    operator fun invoke(): Flow<List<Alert>> {
        return repository.getAlerts()
    }
}