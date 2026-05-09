package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.LpSimulationResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class SimulateLpUseCase @Inject constructor() {
    /**
     * Simulates Impermanent Loss for a 50/50 pool.
     * priceChange: 1.5 for 50% increase, 0.5 for 50% decrease.
     */
    operator fun invoke(
        initialInvestment: Double,
        priceChangeAssetA: Double,
        priceChangeAssetB: Double,
        apy: Double,
        days: Int
    ): LpSimulationResult {
        // Relative price change
        val priceRatio = priceChangeAssetB / priceChangeAssetA
        
        // IL Formula: 2 * sqrt(ratio) / (1 + ratio) - 1
        val il = (2 * sqrt(priceRatio)) / (1 + priceRatio) - 1
        
        // Final value without IL (HODL both assets)
        val hodlValue = initialInvestment * 0.5 * priceChangeAssetA + initialInvestment * 0.5 * priceChangeAssetB
        
        // Final value in pool
        val poolValue = hodlValue * (1 + il)
        
        // Gain from APY (simple)
        val yieldGain = poolValue * (apy / 100.0) * (days / 365.0)
        
        val finalValueWithYield = poolValue + yieldGain
        
        return LpSimulationResult(
            initialValue = initialInvestment,
            finalValue = poolValue,
            impermanentLoss = il * 100, // percentage
            gainWithYield = yieldGain,
            netProfit = finalValueWithYield - initialInvestment
        )
    }
}
