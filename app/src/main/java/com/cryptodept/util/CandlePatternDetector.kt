package com.cryptodept.util

import com.cryptodept.domain.model.OHLCData

/**
 * Utility to detect classic candle patterns for Agent-Sentinel.
 */
class CandlePatternDetector {

    fun detectPatterns(candles: List<OHLCData>): List<String> {
        if (candles.size < 3) return emptyList()
        
        val patterns = mutableListOf<String>()
        val last = candles.last()
        val prev = candles[candles.size - 2]
        
        // 1. Bullish Engulfing
        if (prev.close < prev.open && // prev is red
            last.close > last.open && // last is green
            last.open <= prev.close && 
            last.close >= prev.open) {
            patterns.add("Bullish Engulfing")
        }
        
        // 2. Bearish Engulfing
        if (prev.close > prev.open && // prev is green
            last.close < last.open && // last is red
            last.open >= prev.close &&
            last.close <= prev.open) {
            patterns.add("Bearish Engulfing")
        }
        
        // 3. Hammer (Bullish Reversal)
        val bodySize = Math.abs(last.close - last.open)
        val lowerShadow = Math.min(last.open, last.close) - last.low
        val upperShadow = last.high - Math.max(last.open, last.close)
        if (lowerShadow > (bodySize * 2) && upperShadow < (bodySize * 0.5)) {
            patterns.add("Hammer")
        }

        return patterns
    }
}
