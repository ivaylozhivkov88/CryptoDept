package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StrategyEngine @Inject constructor() {

    fun evaluateEntry(strategy: TradingStrategy, snapshot: MarketDataSnapshot): Boolean {
        return evaluateRules(strategy.entryRules, snapshot)
    }

    fun evaluateExit(strategy: TradingStrategy, snapshot: MarketDataSnapshot): Boolean {
        return evaluateRules(strategy.exitRules, snapshot)
    }

    private fun evaluateRules(rules: List<StrategyRule>, snapshot: MarketDataSnapshot): Boolean {
        if (rules.isEmpty()) return false

        var overallTriggered = true

        rules.forEachIndexed { index, rule ->
            val indicatorValue = getIndicatorValue(rule.indicator, snapshot)
            val rulePassed = compare(indicatorValue, rule.operator, rule.value)

            if (index == 0) {
                overallTriggered = rulePassed
            } else {
                when (rule.logicOperator) {
                    "AND" -> overallTriggered = overallTriggered && rulePassed
                    "OR" -> overallTriggered = overallTriggered || rulePassed
                }
            }
        }
        return overallTriggered
    }

    private fun getIndicatorValue(indicator: String, snapshot: MarketDataSnapshot): Double {
        return when (indicator.uppercase()) {
            "RSI" -> snapshot.rsi
            "PRICE" -> snapshot.price
            "CHANGE_24H" -> snapshot.priceChange24h
            "FEAR_GREED" -> snapshot.fearGreedIndex.toDouble()
            "RISK_SCORE" -> snapshot.riskScore.toDouble()
            "FUNDING_RATE" -> snapshot.fundingRate
            else -> 0.0
        }
    }

    private fun compare(current: Double, operator: String, target: Double): Boolean {
        return when (operator) {
            ">" -> current > target
            "<" -> current < target
            ">=" -> current >= target
            "<=" -> current <= target
            "==" -> current == target
            else -> false
        }
    }
}
