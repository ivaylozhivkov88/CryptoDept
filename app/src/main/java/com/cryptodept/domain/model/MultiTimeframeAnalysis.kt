package com.cryptodept.domain.model

data class TimeframeSignal(
    val timeframe: String, // "15m", "1H", "4H", "1D", "1W"
    val trend: TrendDirection,
    val rsi: Double,
    val macdSignal: MTFMacdSignal,
    val emaSignal: EmaSignal,
    val overallSignal: OverallSignal,
    val candleCount: Int, // Брой свещи изчислено
)

enum class TrendDirection(
    val label: String,
    val icon: String,
) {
    STRONG_UP("STRONG BULL", "▲▲"),
    UP("BULLISH", "▲"),
    SIDEWAYS("NEUTRAL", "━"),
    DOWN("BEARISH", "▼"),
    STRONG_DOWN("STRONG BEAR", "▼▼"),
}

enum class MTFMacdSignal { BULLISH_CROSS, BULLISH, NEUTRAL, BEARISH, BEARISH_CROSS }

enum class EmaSignal { ABOVE_ALL, ABOVE_50, MIXED, BELOW_50, BELOW_ALL }

enum class OverallSignal { STRONG_BUY, BUY, NEUTRAL, SELL, STRONG_SELL }

data class MTFConsensus(
    val timeframes: List<TimeframeSignal>,
    val bullishCount: Int,
    val bearishCount: Int,
    val neutralCount: Int,
    val consensus: OverallSignal,
    val interpretation: String,
    val tradingBias: String, // "LONG", "SHORT", "WAIT"
)
