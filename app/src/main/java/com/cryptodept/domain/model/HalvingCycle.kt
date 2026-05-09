package com.cryptodept.domain.model

data class HalvingCycle(
    val cycleNumber: Int,
    val halvingDate: Long,
    val daysSinceHalving: Long,
    val progressToNextHalving: Float, // 0.0 to 1.0
    val currentPhase: CyclePhase,
    val estimatedNextHalving: Long,
)

enum class CyclePhase(
    val label: String,
    val description: String,
) {
    ACCUMULATION("ACCUMULATION", "Smart money is buying. Volatility is low, preparing for the next leg up."),
    BULL_EARLY("BULL_MARKET_PHASE_1", "Initial breakout. Momentum building as public interest returns."),
    BULL_LATE("BULL_MARKET_PEAK", "Extreme euphoria. Institutional exit liquidity being provided by retail."),
    BEAR("BEAR_MARKET", "Correction and deleveraging. Finding a long-term bottom."),
}
