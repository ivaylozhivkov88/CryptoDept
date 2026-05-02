package com.cryptodept.domain.model

data class PositionSizeResult(
    val portfolioSize: Double,
    val riskPercent: Double,
    val maxLossUsd: Double,
    val entryPrice: Double,
    val stopLossPrice: Double,
    val takeProfitPrice: Double,
    val positionSizeCoins: Double,     // Брой монети
    val positionSizeUsd: Double,       // USD стойност
    val leverageNeeded: Double,        // 1.0 = без ливъридж
    val riskRewardRatio: Double,
    val potentialGainUsd: Double,
    val potentialLossUsd: Double,
    val distanceToSLPercent: Double,
    val distanceToTPPercent: Double,
    val riskAdjustedSize: Double,      // Намален размер при HIGH RISK
    val riskAdjustmentReason: String,
    val grade: PositionGrade
)

enum class PositionGrade(val label: String, val color: Long) {
    EXCELLENT("EXCELLENT SETUP", 0xFF00FF41),   // R:R > 3:1
    GOOD("GOOD SETUP", 0xFF39FF14),             // R:R 2-3:1
    ACCEPTABLE("ACCEPTABLE", 0xFFFFB000),        // R:R 1.5-2:1
    POOR("POOR R:R RATIO", 0xFFFF6600),          // R:R 1-1.5:1
    INVALID("INVALID — R:R < 1:1", 0xFFFF3B30)  // R:R < 1:1
}
