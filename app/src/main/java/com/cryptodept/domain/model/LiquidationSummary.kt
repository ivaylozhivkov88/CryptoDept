package com.cryptodept.domain.model

data class LiquidationSummary(
    val symbol: String,
    val currentPrice: Double,
    val nearestLongLevel: Double,   // closest long liq level BELOW current price
    val totalLongLiquidity: Double, // sum of all long liq within range (USD)
    val nearestShortLevel: Double,  // closest short liq level ABOVE current price
    val totalShortLiquidity: Double,
    val longDominance: Float        // 0f..1f — ratio for progress bar
)
