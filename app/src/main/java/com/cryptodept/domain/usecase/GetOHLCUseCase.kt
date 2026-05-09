package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.OHLCData
import com.cryptodept.domain.repository.ChartRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetOHLCUseCase
    @Inject
    constructor(
        private val repository: ChartRepository,
    ) {
        operator fun invoke(
            coinId: String,
            days: Int,
        ): Flow<List<OHLCData>> = repository.getOHLCData(coinId, days)
    }
