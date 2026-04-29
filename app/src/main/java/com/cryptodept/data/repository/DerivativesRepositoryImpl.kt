package com.cryptodept.data.repository

import com.cryptodept.domain.model.*
import com.cryptodept.domain.repository.DerivativesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DerivativesRepositoryImpl @Inject constructor() : DerivativesRepository {

    override suspend fun getFundingRate(symbol: String): Result<FundingRateData> {
        return try {
            val mockData = FundingRateData(
                symbol = symbol,
                binanceRate = 0.01,
                aggregatedRate = 0.012,
                nextFundingTime = System.currentTimeMillis() + 28800000,
                rateLevel = FundingLevel.NORMAL,
                timestamp = System.currentTimeMillis()
            )
            Result.success(mockData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getOpenInterest(symbol: String): Result<OpenInterestData> {
        return try {
            val mockData = OpenInterestData(
                symbol = symbol,
                openInterestUsd = 150000000.0,
                openInterestChange24h = 2.5,
                trend = OITrend.RISING_WITH_PRICE,
                history = emptyList(),
                timestamp = System.currentTimeMillis()
            )
            Result.success(mockData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLiquidationData(symbol: String): Result<LiquidationData> {
        return try {
            val mockData = LiquidationData(
                symbol = symbol,
                longLiquidations24h = 10000000.0,
                shortLiquidations24h = 5000000.0,
                dominantSide = "LONGS",
                heatmapLevels = emptyList(),
                timestamp = System.currentTimeMillis()
            )
            Result.success(mockData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLongShortRatio(symbol: String): Result<Pair<Double, Double>> {
        return try {
            Result.success(1.5 to 0.8)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getLiquidationHeatmap(symbol: String): Flow<LiquidationData> = flow {
        getLiquidationData(symbol).onSuccess { emit(it) }
    }
}
