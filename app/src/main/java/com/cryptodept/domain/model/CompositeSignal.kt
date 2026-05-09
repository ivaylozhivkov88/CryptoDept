package com.cryptodept.domain.model

enum class SignalStrength {
    STRONG_BUY,
    BUY,
    NEUTRAL,
    SELL,
    STRONG_SELL,
}

data class CompositeSignal(
    val strength: SignalStrength,
    val bullishCount: Int,
    val bearishCount: Int,
    val neutralCount: Int,
    val indicators: List<IndicatorStatus>,
    val confidence: Float,
)

data class IndicatorStatus(
    val name: String,
    val value: String,
    val sentiment: Sentiment,
)

enum class Sentiment {
    BULLISH,
    BEARISH,
    NEUTRAL,
}
