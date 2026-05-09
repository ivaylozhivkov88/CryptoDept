package com.cryptodept.domain.model

data class TradingStrategy(
    val id: String,
    val name: String,
    val description: String,
    val entryRules: List<StrategyRule>,
    val exitRules: List<StrategyRule>
)

data class StrategyRule(
    val indicator: String, // e.g., "RSI", "PRICE", "VOLUME"
    val operator: String, // e.g., "<", ">", "=="
    val value: Double,
    val logicOperator: String? = "AND"
)

enum class StrategyAction {
    BUY, SELL, HOLD
}

data class StrategyEvaluationResult(
    val isTriggered: Boolean,
    val triggerDescription: String,
    val action: StrategyAction
)
