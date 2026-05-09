package com.cryptodept.util

object AppConstants {
    // Technical Analysis Thresholds
    object TA {
        const val RSI_OVERBOUGHT = 70.0
        const val RSI_OVERSOLD = 30.0
        const val RSI_NEUTRAL = 50.0
        
        const val VOL_SPIKE_THRESHOLD = 2.0
        const val VOL_DRY_THRESHOLD = 0.5
        
        const val DOJI_BODY_RATIO = 0.05
        const val HAMMER_WICK_RATIO = 2.0
        
        const val EMA_SHORT = 50
        const val EMA_LONG = 200
        const val RSI_PERIOD = 14
        const val BOLLINGER_PERIOD = 20
        const val BOLLINGER_DEVIATION = 2.0

        // MACD Periods
        const val MACD_FAST = 12
        const val MACD_SLOW = 26
        const val MACD_SIGNAL = 9

        // Analysis Scores
        object Scores {
            const val RSI_HIGH = 85
            const val RSI_LOW = 15
            const val RSI_MID = 50
            
            const val MACD_BULLISH = 75
            const val MACD_BEARISH = 25
            const val MACD_FLAT = 50
            
            const val TREND_STRONG_BULLISH = 90
            const val TREND_STRONG_BEARISH = 10
            const val TREND_BULLISH_BIAS = 60
            const val TREND_BEARISH_BIAS = 40
            
            const val VOL_SPIKE = 80
            const val VOL_DRY = 30
            const val VOL_NORMAL = 50
        }

        // Fibonacci Levels
        object Fibonacci {
            const val LEVEL_0 = 0.0
            const val LEVEL_236 = 0.236
            const val LEVEL_382 = 0.382
            const val LEVEL_500 = 0.500
            const val LEVEL_618 = 0.618
            const val LEVEL_786 = 0.786
            const val LEVEL_100 = 1.0
        }
    }

    // Prediction Models
    object Prediction {
        const val MIN_DATA_POINTS = 50
        const val ELLIOTT_WAVE_MIN_PIVOTS = 5
        const val ELLIOTT_WAVE_WINDOW = 5
        const val ELLIOTT_WAVE_CONFIDENCE = 0.65f
        
        const val BULLISH_TARGET_MULTIPLIER = 1.05
        const val BEARISH_TARGET_MULTIPLIER = 0.95
    }

    // Auth & Security
    object Auth {
        const val ADMIN_CODE = "BIGBOSSBAIKO"
    }

    // Risk Engine Constants
    object Risk {
        const val RSI_EXTREME_RISK = 80.0
        const val RSI_HIGH_RISK = 70.0
        const val RSI_ELEVATED_RISK = 60.0
        
        const val FUNDING_EXTREME = 0.10
        const val FUNDING_HIGH = 0.05
        const val FUNDING_ELEVATED = 0.02
        
        const val LS_RATIO_EXTREME = 3.0
        const val LS_RATIO_HIGH = 2.0
        const val LS_RATIO_ELEVATED = 1.5
        
        const val FG_EXTREME_GREED = 85
        const val FG_GREED = 70
        const val FG_NEUTRAL_MIN = 45
        const val FG_FEAR_MAX = 25
        
        const val INFLOW_MASSIVE = 50.0
        const val INFLOW_HIGH = 20.0
        const val INFLOW_ELEVATED = 5.0
    }

    // Cache & Network
    object Network {
        const val RATE_LIMIT_MS = 10_000L
        const val CACHE_DURATION_MS = 10 * 60 * 1000L // 10 minutes
        const val CONNECT_TIMEOUT_SECONDS = 30L
        const val READ_TIMEOUT_SECONDS = 30L
    }
}
