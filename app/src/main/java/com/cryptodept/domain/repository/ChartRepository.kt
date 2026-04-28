package com.cryptodept.domain.repository

import com.cryptodept.domain.model.OHLCData
import kotlinx.coroutines.flow.Flow

interface ChartRepository {
    fun getOHLCData(coinId: String, days: Int): Flow<List<OHLCData>>
    suspend fun refreshOHLCData(coinId: String, days: Int): Result<Unit>
}