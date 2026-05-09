package com.cryptodept.domain.model

/**
 * PROMPT #138 — Advanced Signal Composer
 * Part A: Domain models for technical indicators (RSI, MACD, VolumeChange)
 */

// ════════════════════════════════════════════════════════════════
// Technical Indicator Values (calculated from price history)
// ════════════════════════════════════════════════════════════════

data class RSIIndicator(
    val value: Float, // 0-100
    val period: Int = 14,
    val overBought: Float = 70f,
    val overSold: Float = 30f,
) {
    fun getLevel(): RSILevel =
        when {
            value >= overBought -> RSILevel.OVERBOUGHT
            value > overSold && value < overBought -> RSILevel.NEUTRAL
            value <= overSold -> RSILevel.OVERSOLD
            else -> RSILevel.INVALID
        }
}

enum class RSILevel {
    OVERBOUGHT,
    NEUTRAL,
    OVERSOLD,
    INVALID,
}

data class MACDIndicator(
    val macdLine: Float, // MACD line
    val signalLine: Float, // Signal line (9-period EMA of MACD)
    val histogram: Float, // MACD - Signal (divergence)
    val fastPeriod: Int = 12,
    val slowPeriod: Int = 26,
    val signalPeriod: Int = 9,
) {
    fun getSignal(): MACDSignal =
        when {
            histogram > 0 && macdLine > signalLine -> MACDSignal.BULLISH_CROSSING
            histogram < 0 && macdLine < signalLine -> MACDSignal.BEARISH_CROSSING
            histogram > 0 -> MACDSignal.POSITIVE_HISTOGRAM
            histogram < 0 -> MACDSignal.NEGATIVE_HISTOGRAM
            else -> MACDSignal.NEUTRAL
        }
}

enum class MACDSignal {
    BULLISH_CROSSING,
    BEARISH_CROSSING,
    POSITIVE_HISTOGRAM,
    NEGATIVE_HISTOGRAM,
    NEUTRAL,
}

data class VolumeIndicator(
    val volumeChange24h: Double, // % change in 24h volume
    val volumeAvg14: Double, // 14-day average volume
    val currentVolume: Double, // current 24h volume
    val volumeMultiplier: Double = 0.0, // current / average ratio
) {
    fun getStrength(): VolumeStrength =
        when {
            volumeMultiplier > 1.5 -> VolumeStrength.VERY_HIGH
            volumeMultiplier > 1.2 -> VolumeStrength.HIGH
            volumeMultiplier > 0.8 -> VolumeStrength.NORMAL
            else -> VolumeStrength.LOW
        }
}

enum class VolumeStrength {
    VERY_HIGH,
    HIGH,
    NORMAL,
    LOW,
}

// ════════════════════════════════════════════════════════════════
// Combined Technical Analysis Snapshot
// ════════════════════════════════════════════════════════════════

data class TechnicalSnapshot(
    val coinSymbol: String,
    val timestamp: Long = System.currentTimeMillis(),
    val rsi: RSIIndicator?,
    val macd: MACDIndicator?,
    val volume: VolumeIndicator?,
    val price: Double,
    val priceChange24h: Double, // % change
)
