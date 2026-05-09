package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.*
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertEngine
    @Inject
    constructor() {
        fun evaluateCompositeAlert(
            alert: CompositeAlert,
            currentPrice: Double,
            currentRSI: Double,
            currentVolume: Double,
            volumeChangePercent: Double,
            currentMACD: Double,
            fundingRate: Double,
        ): AlertEvaluationResult {
            val conditionResults =
                alert.conditions.map { condition ->
                    evaluateCondition(
                        condition = condition,
                        currentPrice = currentPrice,
                        currentRSI = currentRSI,
                        currentVolume = currentVolume,
                        volumeChangePercent = volumeChangePercent,
                        currentMACD = currentMACD,
                        fundingRate = fundingRate,
                    )
                }

            val overallResult =
                when (alert.logicOperator) {
                    AlertLogicOperator.AND -> conditionResults.all { it.isMet }
                    AlertLogicOperator.OR -> conditionResults.any { it.isMet }
                }

            return AlertEvaluationResult(
                alertId = alert.id,
                conditionResults = conditionResults,
                overallResult = overallResult,
            )
        }

        private fun evaluateCondition(
            condition: AlertCondition,
            currentPrice: Double,
            currentRSI: Double,
            currentVolume: Double,
            volumeChangePercent: Double,
            currentMACD: Double,
            fundingRate: Double,
        ): ConditionResult {
            val currentValue =
                when (condition.type) {
                    AlertConditionType.PRICE -> currentPrice
                    AlertConditionType.RSI -> currentRSI
                    AlertConditionType.VOLUME -> volumeChangePercent
                    AlertConditionType.MACD -> currentMACD
                    AlertConditionType.FUNDING_RATE -> fundingRate
                }

            val isMet =
                when (condition.operator) {
                    AlertDirection.ABOVE -> currentValue > condition.targetValue
                    AlertDirection.BELOW -> currentValue < condition.targetValue
                }

            return ConditionResult(
                conditionId = condition.id,
                type = condition.type,
                currentValue = currentValue,
                targetValue = condition.targetValue,
                isMet = isMet,
            )
        }

        fun shouldTriggerAlert(
            alert: CompositeAlert,
            evaluation: AlertEvaluationResult,
        ): Boolean {
            if (!alert.isActive || !evaluation.overallResult) return false

            // Check cooldown
            val now = System.currentTimeMillis()
            val lastTriggered = alert.lastTriggeredAt ?: 0L
            val cooldownMs = alert.cooldownMinutes * 60 * 1000

            return (now - lastTriggered) >= cooldownMs
        }

        fun buildAlertDescription(alert: CompositeAlert): String {
            val sb = StringBuilder()
            sb.append("${alert.name}\n")
            sb.append("${alert.coinSymbol} | Logic: ${alert.logicOperator.name}\n\n")

            alert.conditions.forEach { condition ->
                val operator =
                    when (condition.operator) {
                        AlertDirection.ABOVE -> ">"
                        AlertDirection.BELOW -> "<"
                    }
                sb.append("• ${condition.type.name} $operator ${condition.targetValue}\n")
            }

            return sb.toString()
        }

        fun createSimpleAlert(
            coinId: String,
            coinSymbol: String,
            priceTarget: Double,
        ): CompositeAlert =
            CompositeAlert(
                name = "$coinSymbol Price Alert",
                coinId = coinId,
                coinSymbol = coinSymbol,
                conditions =
                    listOf(
                        AlertCondition(
                            type = AlertConditionType.PRICE,
                            operator = AlertDirection.ABOVE,
                            targetValue = priceTarget,
                            description = "Price > $$priceTarget",
                        ),
                    ).toImmutableList(),
                logicOperator = AlertLogicOperator.AND,
            )

        fun createAdvancedAlert(
            coinId: String,
            coinSymbol: String,
            priceTarget: Double,
            rsiTarget: Double,
            volumeIncreasePercent: Double,
        ): CompositeAlert =
            CompositeAlert(
                name = "$coinSymbol Advanced Setup",
                coinId = coinId,
                coinSymbol = coinSymbol,
                conditions =
                    listOf(
                        AlertCondition(
                            type = AlertConditionType.PRICE,
                            operator = AlertDirection.ABOVE,
                            targetValue = priceTarget,
                            description = "Price > $$priceTarget",
                        ),
                        AlertCondition(
                            type = AlertConditionType.RSI,
                            operator = AlertDirection.ABOVE,
                            targetValue = rsiTarget,
                            description = "RSI > $rsiTarget",
                        ),
                        AlertCondition(
                            type = AlertConditionType.VOLUME,
                            operator = AlertDirection.ABOVE,
                            targetValue = volumeIncreasePercent,
                            description = "Volume +$volumeIncreasePercent%",
                        ),
                    ).toImmutableList(),
                logicOperator = AlertLogicOperator.AND,
            )
    }
