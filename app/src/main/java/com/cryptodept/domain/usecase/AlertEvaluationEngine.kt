package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PROMPT #127 — Custom Composite Alerts
 * Part A: Alert Evaluation Engine
 */
@Singleton
class AlertEvaluationEngine
    @Inject
    constructor() {
        fun evaluate(
            alert: CompositeAlert,
            snapshot: TechnicalSnapshot,
        ): AlertEvaluationResult {
            val results =
                alert.conditions.map { condition ->
                    evaluateCondition(condition, snapshot)
                }

            val overall =
                when (alert.logicOperator) {
                    AlertLogicOperator.AND -> results.isNotEmpty() && results.all { it.isMet }
                    AlertLogicOperator.OR -> results.any { it.isMet }
                }

            return AlertEvaluationResult(
                alertId = alert.id,
                conditionResults = results,
                overallResult = overall,
            )
        }

        private fun evaluateCondition(
            condition: AlertCondition,
            snapshot: TechnicalSnapshot,
        ): ConditionResult {
            val currentValue: Double =
                when (condition.type) {
                    AlertConditionType.PRICE -> snapshot.price
                    AlertConditionType.RSI -> (snapshot.rsi?.value ?: 50.0).toDouble()
                    AlertConditionType.VOLUME -> snapshot.volume?.volumeMultiplier ?: 1.0
                    AlertConditionType.MACD -> (snapshot.macd?.histogram ?: 0.0f).toDouble()
                    AlertConditionType.FUNDING_RATE -> 0.0 // Placeholder
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
    }
