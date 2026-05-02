package com.cryptodept.domain.model

data class TradeSetup(
    val coin: String,
    val direction: TradeDirectionType,
    val entryPrice: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val checklist: List<ChecklistItem>,
    val score: Int,          // 0-10 passed checks
    val maxScore: Int,       // Total checks
    val verdict: SetupVerdict
)

data class ChecklistItem(
    val category: String,    // "TREND", "CONFLUENCE", "RISK", "MARKET"
    val description: String,
    val isPassed: Boolean,
    val isCritical: Boolean  // Критичен = при fail → override to AVOID
)

enum class TradeDirectionType { LONG, SHORT }
enum class SetupVerdict {
    STRONG_SETUP,    // 8-10/10
    GOOD_SETUP,      // 6-7/10
    PROCEED_CAUTION, // 4-5/10
    AVOID            // <4 или critical fail
}
