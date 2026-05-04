package com.cryptodept.domain.model

enum class AlertDirection {
    ABOVE, BELOW
}

enum class AlertConditionType {
    PRICE, RSI, VOLUME, MACD, FUNDING_RATE
}

enum class AlertLogicOperator {
    AND, OR
}

data class Alert(
    val id: Int = 0,
    val coinId: String,
    val coinSymbol: String,
    val targetPrice: Double,
    val direction: AlertDirection,
    val isActive: Boolean = true,
    val isTriggered: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class CompositeAlert(
    val id: Int = 0,
    val name: String,
    val coinId: String,
    val coinSymbol: String,
    val conditions: List<AlertCondition>,
    val logicOperator: AlertLogicOperator = AlertLogicOperator.AND,
    val isActive: Boolean = true,
    val isTriggered: Boolean = false,
    val triggerCount: Int = 0,
    val cooldownMinutes: Int = 60,
    val lastTriggeredAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class AlertCondition(
    val id: Int = 0,
    val type: AlertConditionType,
    val operator: AlertDirection, // ABOVE or BELOW
    val targetValue: Double,
    val description: String // "RSI > 70", "Volume +20%", etc
)

data class AlertEvaluationResult(
    val alertId: Int,
    val conditionResults: List<ConditionResult>,
    val overallResult: Boolean,
    val evaluatedAt: Long = System.currentTimeMillis()
)

data class ConditionResult(
    val conditionId: Int,
    val type: AlertConditionType,
    val currentValue: Double,
    val targetValue: Double,
    val isMet: Boolean
)