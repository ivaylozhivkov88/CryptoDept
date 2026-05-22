package com.cryptodept.domain.repository

import com.cryptodept.data.api.LiquidationMapResponse

interface CoinGlassRepository {
    suspend fun getLiquidationMap(symbol: String): Result<LiquidationMapResponse>
}
