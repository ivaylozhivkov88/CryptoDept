package com.cryptodept.domain.repository

import com.cryptodept.domain.model.FundingHeatmapItem
import com.cryptodept.domain.model.FundingRateData
import com.cryptodept.domain.model.LiquidationData
import com.cryptodept.domain.model.OpenInterestData
import com.cryptodept.domain.model.DerivativesSnapshot
import kotlinx.coroutines.flow.Flow

interface DerivativesRepository {
    suspend fun getDerivativesSnapshot(symbol: String): DerivativesSnapshot

    suspend fun getFundingRate(symbol: String): Result<FundingRateData>

    suspend fun getOpenInterest(symbol: String): Result<OpenInterestData>

    suspend fun getLiquidationData(symbol: String): Result<LiquidationData>

    suspend fun getLongShortRatio(symbol: String): Result<Pair<Double, Double>>

    suspend fun getFundingHeatmap(): Result<List<FundingHeatmapItem>>

    fun getLiquidationHeatmap(symbol: String): Flow<LiquidationData>
}
