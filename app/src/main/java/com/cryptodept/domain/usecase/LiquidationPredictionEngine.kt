package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.LiquidationData
import com.cryptodept.domain.model.LiquidationType
import com.cryptodept.domain.model.MagneticZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class LiquidationPredictionEngine @Inject constructor() {
    fun predictMagneticZones(currentPrice: Double, data: LiquidationData): List<MagneticZone> {
        if (currentPrice <= 0) return emptyList()
        
        return data.heatmapLevels
            .filter { it.isSignificant }
            .map { level ->
                val total = level.longLiquidationUsd + level.shortLiquidationUsd
                val dist = ((level.price - currentPrice) / currentPrice) * 100
                val type = if (level.price > currentPrice) {
                    LiquidationType.SHORT_SQUEEZE_POTENTIAL
                } else {
                    LiquidationType.LONG_SQUEEZE_POTENTIAL
                }
                MagneticZone(level.price, total, dist, type)
            }
            .sortedBy { abs(it.distancePercent) }
            .take(3)
    }
}
