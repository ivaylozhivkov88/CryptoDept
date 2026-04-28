// STEP 13: RSI Calculation Use Case
// Created: 2024-05-22
// Dependencies: OHLCData
// Used by: AnalysisViewModel

package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.OHLCData
import javax.inject.Inject
import kotlin.math.abs

class GetRsiUseCase @Inject constructor() {
    
    /**
     * Calculates RSI for a given list of OHLC data.
     * Formula: RSI = 100 - [100 / (1 + RS)]
     * RS = Average Gain / Average Loss
     */
    fun execute(data: List<OHLCData>, period: Int = 14): Double? {
        if (data.size <= period) return null

        val closes = data.map { it.close }
        val changes = closes.zipWithNext { a, b -> b - a }

        var avgGain = changes.take(period).filter { it > 0 }.sum() / period
        var avgLoss = changes.take(period).filter { it < 0 }.map { abs(it) }.sum() / period

        if (avgLoss == 0.0) return 100.0

        // Wilders Smoothing
        for (i in period until changes.size) {
            val change = changes[i]
            val gain = if (change > 0) change else 0.0
            val loss = if (change < 0) abs(change) else 0.0

            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period
        }

        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }
}
