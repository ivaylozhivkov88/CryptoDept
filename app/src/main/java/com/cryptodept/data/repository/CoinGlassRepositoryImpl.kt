package com.cryptodept.data.repository

import com.cryptodept.data.api.CoinglassApi
import com.cryptodept.data.api.LiquidationMapResponse
import com.cryptodept.domain.repository.CoinGlassRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoinGlassRepositoryImpl @Inject constructor(
    private val coinglassApi: CoinglassApi
) : CoinGlassRepository {

    override suspend fun getLiquidationMap(symbol: String): Result<LiquidationMapResponse> = try {
        val response = coinglassApi.getLiquidationMap(symbol)
        if (response.isSuccessful && response.body() != null) {
            Result.success(response.body()!!)
        } else {
            Result.failure(Exception("API Error: ${response.code()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
