package com.cryptodept.domain.model

data class AnalysisTrace(
    val factor: String, // "RSI", "MACD", "EMA", "VOL"
    val score: Int, // 0-100
    val label: String, // "OVERBOUGHT", "CROSSOVER"
    val reasoning: String, // "RSI at 82 suggests buyers are exhausted."
    val intensity: TraceIntensity = TraceIntensity.MEDIUM,
)

enum class TraceIntensity {
    LOW,
    MEDIUM,
    HIGH,
    EXTREME,
}

data class ScoreBreakdown(
    val overallScore: Int,
    val traces: List<AnalysisTrace>,
)
