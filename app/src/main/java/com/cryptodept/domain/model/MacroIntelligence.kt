package com.cryptodept.domain.model

/**
 * High-level market intelligence data for dashboard display.
 */
data class MacroIntelligence(
    val btcDominance: Double,
    val btcDominanceDelta24h: Double,
    val ethGasGwei: Int,
    val globalMarketCapUsd: Double,
    val altcoinSeasonIndex: Int, // 0-100
    val globalLiquidityUsd: Double = 0.0,
    val gasPrediction: String = "NOW", // NEW: M1.2
    val totalLiquidations1h: LiquidationSnapshot,
    val totalLiquidations24h: LiquidationSnapshot
)

data class LiquidationSnapshot(
    val totalUsd: Double,
    val longsUsd: Double,
    val shortsUsd: Double,
    val timestamp: Long
)

enum class MarketCycle {
    BTC_SEASON,
    NEUTRAL,
    ALT_SEASON
}
