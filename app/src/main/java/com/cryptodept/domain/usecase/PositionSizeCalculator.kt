package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.PositionGrade
import com.cryptodept.domain.model.PositionSizeResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PositionSizeCalculator @Inject constructor() {
    fun calculate(
        portfolioSize: Double,       // USD
        riskPercent: Double,         // % от портфолио (препоръчително 1-2%)
        entryPrice: Double,
        stopLossPrice: Double,
        takeProfitPrice: Double,
        currentRiskScore: Int = 50   // От RiskScoreEngine
    ): PositionSizeResult {

        val maxLossUsd = portfolioSize * (riskPercent / 100.0)
        val distanceToSL = kotlin.math.abs(entryPrice - stopLossPrice)
        val distanceToTP = kotlin.math.abs(takeProfitPrice - entryPrice)

        // Основен размер на позицията
        val positionSizeCoins = if (distanceToSL > 0) maxLossUsd / distanceToSL else 0.0
        val positionSizeUsd = positionSizeCoins * entryPrice
        val leverageNeeded = if (portfolioSize > 0) positionSizeUsd / portfolioSize else 0.0

        val riskRewardRatio = if (distanceToSL > 0) distanceToTP / distanceToSL else 0.0
        val potentialGainUsd = positionSizeCoins * distanceToTP
        val potentialLossUsd = maxLossUsd

        val distanceToSLPercent = if (entryPrice > 0) (distanceToSL / entryPrice) * 100 else 0.0
        val distanceToTPPercent = if (entryPrice > 0) (distanceToTP / entryPrice) * 100 else 0.0

        // Risk-adjusted размер (намалява при висок Risk Score)
        val riskMultiplier = when {
            currentRiskScore >= 80 -> 0.50  // Extreme risk → половин размер
            currentRiskScore >= 70 -> 0.70  // High risk → 70%
            currentRiskScore >= 60 -> 0.85  // Moderate → 85%
            else -> 1.0                      // Normal → пълен размер
        }
        val riskAdjustedSize = positionSizeUsd * riskMultiplier
        val riskAdjustmentReason = when {
            currentRiskScore >= 80 -> "EXTREME RISK (${currentRiskScore}/100) — Size reduced 50%"
            currentRiskScore >= 70 -> "HIGH RISK (${currentRiskScore}/100) — Size reduced 30%"
            currentRiskScore >= 60 -> "MODERATE RISK (${currentRiskScore}/100) — Size reduced 15%"
            else -> "Normal market conditions — Full size applied"
        }

        val grade = when {
            riskRewardRatio < 1.0  -> PositionGrade.INVALID
            riskRewardRatio < 1.5  -> PositionGrade.POOR
            riskRewardRatio < 2.0  -> PositionGrade.ACCEPTABLE
            riskRewardRatio < 3.0  -> PositionGrade.GOOD
            else                   -> PositionGrade.EXCELLENT
        }

        return PositionSizeResult(
            portfolioSize, riskPercent, maxLossUsd, entryPrice,
            stopLossPrice, takeProfitPrice, positionSizeCoins,
            positionSizeUsd, leverageNeeded, riskRewardRatio,
            potentialGainUsd, potentialLossUsd, distanceToSLPercent,
            distanceToTPPercent, riskAdjustedSize, riskAdjustmentReason, grade
        )
    }
}
