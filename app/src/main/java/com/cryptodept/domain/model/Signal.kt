package com.cryptodept.domain.model

/**
 * PROMPT #138 — Advanced Signal Composer
 * Part A: Domain models for custom trading signal rules
 */

// ════════════════════════════════════════════════════════════════
// Signal Condition Types (building blocks for rules)
// ════════════════════════════════════════════════════════════════

sealed class SignalCondition {
    abstract val weight: Int // 1-10: importance factor

    // RSI-based conditions
    data class RSIAbove(
        val threshold: Float,
        override val weight: Int = 5,
    ) : SignalCondition()

    data class RSIBelow(
        val threshold: Float,
        override val weight: Int = 5,
    ) : SignalCondition()

    data class RSIOverbought(
        override val weight: Int = 7,
    ) : SignalCondition()

    data class RSIOversold(
        override val weight: Int = 7,
    ) : SignalCondition()

    // MACD-based conditions
    data class MACDBullishCrossing(
        override val weight: Int = 8,
    ) : SignalCondition()

    data class MACDBearishCrossing(
        override val weight: Int = 8,
    ) : SignalCondition()

    data class MACDHistogramPositive(
        override val weight: Int = 5,
    ) : SignalCondition()

    data class MACDHistogramNegative(
        override val weight: Int = 5,
    ) : SignalCondition()

    // Volume-based conditions
    data class VolumeAboveAverage(
        val multiplier: Double = 1.2,
        override val weight: Int = 4,
    ) : SignalCondition()

    data class VolumeSpike(
        override val weight: Int = 6,
    ) : SignalCondition()

    // Price-based conditions
    data class PriceAboveMA(
        val period: Int = 50,
        override val weight: Int = 3,
    ) : SignalCondition()

    data class PriceChange(
        val percentThreshold: Double,
        override val weight: Int = 4,
    ) : SignalCondition() // positive or negative
}

// ════════════════════════════════════════════════════════════════
// Signal Rule & Verdict
// ════════════════════════════════════════════════════════════════

data class SignalRule(
    val id: String, // user-defined rule ID
    val name: String, // "Bullish RSI Divergence", "Safe Entry"
    val conditions: List<SignalCondition>, // AND combination (all must be true)
    val signal: TradeSignal, // BUY / SELL / NEUTRAL
    val confidence: Int = 50, // 0-100, based on condition weights
    val isEnabled: Boolean = true,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

enum class TradeSignal {
    STRONG_BUY,
    BUY,
    NEUTRAL,
    SELL,
    STRONG_SELL,
}

data class SignalEvaluation(
    val coinSymbol: String,
    val signal: TradeSignal,
    val confidence: Int, // 0-100
    val matchedRules: List<String>, // which rules triggered
    val reasoning: String, // brief explanation
    val timestamp: Long = System.currentTimeMillis(),
)

// ════════════════════════════════════════════════════════════════
// Evaluation Result (for composition)
// ════════════════════════════════════════════════════════════════

data class SignalCompositionResult(
    val coinSymbol: String,
    val technicalSnapshot: TechnicalSnapshot,
    val evaluations: List<SignalEvaluation>,
    val finalSignal: TradeSignal, // consensus
    val finalConfidence: Int, // weighted average
    val timestamp: Long = System.currentTimeMillis(),
)
