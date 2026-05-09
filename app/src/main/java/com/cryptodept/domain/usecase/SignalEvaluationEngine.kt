package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PROMPT #138 — Advanced Signal Composer
 * Part A: Signal Evaluation Engine
 *
 * Evaluates technical snapshot against user-defined signal rules
 * and returns composite signal with confidence level.
 */
@Singleton
class SignalEvaluationEngine
    @Inject
    constructor() {
        /**
         * Evaluate a technical snapshot against a single rule.
         * Returns SignalEvaluation with confidence based on matched conditions.
         */
        fun evaluateRule(
            rule: SignalRule,
            snapshot: TechnicalSnapshot,
        ): SignalEvaluation {
            if (!rule.isEnabled) {
                return SignalEvaluation(
                    coinSymbol = snapshot.coinSymbol,
                    signal = TradeSignal.NEUTRAL,
                    confidence = 0,
                    matchedRules = emptyList(),
                    reasoning = "Rule disabled",
                )
            }

            var totalWeight = 0
            var matchedWeight = 0
            val results = mutableListOf<Boolean>()

            // Evaluate each condition
            for (condition in rule.conditions) {
                val matches = evaluateCondition(condition, snapshot)
                results.add(matches)

                if (matches) {
                    matchedWeight += condition.weight
                }
                totalWeight += condition.weight
            }

            // All conditions must match (AND logic) for rule to trigger
            val ruleTriggered = results.all { it }

            val confidence =
                if (ruleTriggered && totalWeight > 0) {
                    (matchedWeight * 100 / totalWeight).coerceIn(0, 100)
                } else {
                    0
                }

            return SignalEvaluation(
                coinSymbol = snapshot.coinSymbol,
                signal = if (ruleTriggered) rule.signal else TradeSignal.NEUTRAL,
                confidence = confidence,
                matchedRules = if (ruleTriggered) listOf(rule.id) else emptyList(),
                reasoning =
                    if (ruleTriggered) {
                        "Rule '${rule.name}' triggered. Confidence: $confidence%"
                    } else {
                        "Rule '${rule.name}' not satisfied. ${results.count { it }}/${results.size} conditions met"
                    },
            )
        }

        fun evaluateCustomRule(
            rule: CustomSignalRule,
            snapshot: TechnicalSnapshot,
        ): SignalEvaluation {
            if (!rule.isActive) {
                return SignalEvaluation(snapshot.coinSymbol, TradeSignal.NEUTRAL, 0, emptyList(), "Rule disabled")
            }

            val results = rule.conditions.map { evaluateCustomCondition(it, snapshot) }

            val ruleTriggered =
                if (rule.operator == LogicalOperator.AND) {
                    results.all { it }
                } else {
                    results.any { it }
                }

            return SignalEvaluation(
                coinSymbol = snapshot.coinSymbol,
                signal =
                    if (ruleTriggered) {
                        when (rule.action) {
                            SignalAction.BUY -> TradeSignal.BUY
                            SignalAction.SELL -> TradeSignal.SELL
                            SignalAction.CAUTION -> TradeSignal.NEUTRAL
                        }
                    } else {
                        TradeSignal.NEUTRAL
                    },
                confidence = if (ruleTriggered) 70 else 0, // Simplified confidence for custom rules
                matchedRules = if (ruleTriggered) listOf(rule.id) else emptyList(),
                reasoning = if (ruleTriggered) "Custom rule '${rule.name}' triggered." else "Custom rule not satisfied.",
            )
        }

        private fun evaluateCustomCondition(
            condition: CustomSignalCondition,
            snapshot: TechnicalSnapshot,
        ): Boolean {
            val indicatorValue: Double =
                when (condition.indicator) {
                    IndicatorType.RSI -> (snapshot.rsi?.value ?: 50.0).toDouble()
                    IndicatorType.PRICE -> snapshot.priceChange24h
                    IndicatorType.VOLUME_CHANGE_24H -> snapshot.volume?.volumeMultiplier ?: 1.0
                    else -> 0.0
                }

            return when (condition.operator) {
                ComparisonOperator.GREATER_THAN -> indicatorValue > condition.value
                ComparisonOperator.LESS_THAN -> indicatorValue < condition.value
                else -> false // CROSSES logic would need historical data
            }
        }

        /**
         * Evaluate a single condition against the technical snapshot.
         */
        private fun evaluateCondition(
            condition: SignalCondition,
            snapshot: TechnicalSnapshot,
        ): Boolean =
            when (condition) {
                // RSI conditions
                is SignalCondition.RSIAbove -> {
                    snapshot.rsi?.value?.let { it > condition.threshold } ?: false
                }
                is SignalCondition.RSIBelow -> {
                    snapshot.rsi?.value?.let { it < condition.threshold } ?: false
                }
                is SignalCondition.RSIOverbought -> {
                    snapshot.rsi?.getLevel() == RSILevel.OVERBOUGHT
                }
                is SignalCondition.RSIOversold -> {
                    snapshot.rsi?.getLevel() == RSILevel.OVERSOLD
                }

                // MACD conditions
                is SignalCondition.MACDBullishCrossing -> {
                    snapshot.macd?.getSignal() == MACDSignal.BULLISH_CROSSING
                }
                is SignalCondition.MACDBearishCrossing -> {
                    snapshot.macd?.getSignal() == MACDSignal.BEARISH_CROSSING
                }
                is SignalCondition.MACDHistogramPositive -> {
                    snapshot.macd?.histogram?.let { it > 0 } ?: false
                }
                is SignalCondition.MACDHistogramNegative -> {
                    snapshot.macd?.histogram?.let { it < 0 } ?: false
                }

                // Volume conditions
                is SignalCondition.VolumeAboveAverage -> {
                    snapshot.volume?.volumeMultiplier?.let { it > condition.multiplier } ?: false
                }
                is SignalCondition.VolumeSpike -> {
                    snapshot.volume?.getStrength() == VolumeStrength.VERY_HIGH
                }

                // Price conditions
                is SignalCondition.PriceAboveMA -> {
                    // placeholder: would need MA calculation
                    // for now just check if price is positive direction
                    snapshot.priceChange24h > 0
                }
                is SignalCondition.PriceChange -> {
                    if (condition.percentThreshold > 0) {
                        snapshot.priceChange24h >= condition.percentThreshold
                    } else {
                        snapshot.priceChange24h <= condition.percentThreshold
                    }
                }
            }

        /**
         * Evaluate multiple rules against snapshot and compose final signal.
         */
        fun evaluateComposite(
            rules: List<SignalRule>,
            snapshot: TechnicalSnapshot,
        ): SignalCompositionResult {
            val evaluations =
                rules.map { rule ->
                    evaluateRule(rule, snapshot)
                }

            // Filter only triggered evaluations
            val triggeredEvals = evaluations.filter { it.signal != TradeSignal.NEUTRAL }

            // Compute final signal and confidence
            val (finalSignal, finalConfidence) =
                if (triggeredEvals.isNotEmpty()) {
                    // Count votes for each signal type
                    val signalVotes = triggeredEvals.groupingBy { it.signal }.eachCount()
                    val strongestSignal = signalVotes.maxByOrNull { it.value }?.key ?: TradeSignal.NEUTRAL

                    // Average confidence for the strongest signal
                    val avgConfidence =
                        evaluations
                            .filter { it.signal == strongestSignal }
                            .map { it.confidence }
                            .average()
                            .toInt()

                    strongestSignal to avgConfidence
                } else {
                    TradeSignal.NEUTRAL to 0
                }

            return SignalCompositionResult(
                coinSymbol = snapshot.coinSymbol,
                technicalSnapshot = snapshot,
                evaluations = evaluations,
                finalSignal = finalSignal,
                finalConfidence = finalConfidence,
            )
        }
    }
