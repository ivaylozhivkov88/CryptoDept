package com.cryptodept.domain.model

data class CustomSignalRule(
    val id: String,
    val name: String,
    val conditions: List<CustomSignalCondition>,
    val operator: LogicalOperator = LogicalOperator.AND,
    val action: SignalAction = SignalAction.BUY,
    val isActive: Boolean = true,
)

data class CustomSignalCondition(
    val indicator: IndicatorType,
    val operator: ComparisonOperator,
    val value: Double,
    val timeframe: String = "1h",
)

enum class IndicatorType {
    RSI,
    MACD_MAIN,
    MACD_SIGNAL,
    EMA_50,
    EMA_200,
    PRICE,
    VOLUME_CHANGE_24H,
}

enum class ComparisonOperator {
    GREATER_THAN,
    LESS_THAN,
    CROSSES_ABOVE,
    CROSSES_BELOW,
}

enum class LogicalOperator {
    AND,
    OR,
}

enum class SignalAction {
    BUY,
    SELL,
    CAUTION,
}
