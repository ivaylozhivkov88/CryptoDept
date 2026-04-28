package com.cryptodept.domain.model

data class OHLCData(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
) {
    val isGreen: Boolean get() = close >= open
    val bodySize: Double get() = kotlin.math.abs(close - open)
    val totalSize: Double get() = high - low
    val upperWick: Double get() = high - kotlin.math.max(open, close)
    val lowerWick: Double get() = kotlin.math.min(open, close) - low
}
