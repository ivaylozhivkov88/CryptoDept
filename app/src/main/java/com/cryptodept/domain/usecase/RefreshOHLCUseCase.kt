package com.cryptodept.domain.usecase

import com.cryptodept.domain.repository.ChartRepository
import javax.inject.Inject

class RefreshOHLCUseCase @Inject constructor(
    private val repository: ChartRepository
) {
    suspend operator fun invoke(coinId: String, days: Int): Result<Unit> {
        return repository.refreshOHLCData(coinId, days)
    }
}
