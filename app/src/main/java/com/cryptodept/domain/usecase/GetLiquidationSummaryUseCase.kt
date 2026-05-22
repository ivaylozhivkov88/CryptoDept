package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.LiquidationSummary
import com.cryptodept.domain.repository.CoinGlassRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetLiquidationSummaryUseCase @Inject constructor(
    private val repository: CoinGlassRepository
) {
    suspend operator fun invoke(
        symbol: String,
        currentPrice: Double
    ): Result<LiquidationSummary> {
        return repository.getLiquidationMap(symbol).map { response ->
            val liqList = response.data?.liqList ?: emptyList()
            
            val longLevels = liqList.filter { it.direction == "long" && it.price < currentPrice }
                .sortedByDescending { it.price }
            
            val shortLevels = liqList.filter { it.direction == "short" && it.price > currentPrice }
                .sortedBy { it.price }
            
            val nearestLong = longLevels.firstOrNull()?.price ?: 0.0
            val totalLong = longLevels.sumOf { it.liqSize }
            
            val nearestShort = shortLevels.firstOrNull()?.price ?: 0.0
            val totalShort = shortLevels.sumOf { it.liqSize }
            
            val totalLiquidity = totalLong + totalShort
            val longDominance = if (totalLiquidity > 0) (totalLong / totalLiquidity).toFloat() else 0.5f
            
            LiquidationSummary(
                symbol = symbol,
                currentPrice = currentPrice,
                nearestLongLevel = nearestLong,
                totalLongLiquidity = totalLong,
                nearestShortLevel = nearestShort,
                totalShortLiquidity = totalShort,
                longDominance = longDominance
            )
        }
    }
}
