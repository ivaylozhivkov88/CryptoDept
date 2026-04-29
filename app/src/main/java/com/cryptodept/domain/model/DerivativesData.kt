package com.cryptodept.domain.model

data class FundingRateData(
    val symbol: String,           // "BTC"
    val binanceRate: Double,      // Funding rate от Binance Futures
    val aggregatedRate: Double,   // Средна от всички борси
    val nextFundingTime: Long,
    val rateLevel: FundingLevel,  // Enum за интерпретация
    val timestamp: Long
)

enum class FundingLevel(val description: String, val isBullishWarning: Boolean) {
    VERY_HIGH("Extreme Bullish Sentiment — Crash Risk", true),  // > 0.10%
    HIGH("Elevated Bullish Sentiment — Caution", true),         // 0.05% - 0.10%
    NORMAL("Neutral Funding — Healthy Market", false),          // -0.02% - 0.05%
    LOW("Slight Bearish Sentiment", false),                     // -0.05% - -0.02%
    VERY_LOW("Extreme Bearish Sentiment — Bounce Risk", false)  // < -0.05%
}

data class OpenInterestData(
    val symbol: String,
    val openInterestUsd: Double,
    val openInterestChange24h: Double,  // % промяна
    val trend: OITrend,
    val history: List<OHLCData>,        // Reuse OHLCData с OI стойности
    val timestamp: Long
)

enum class OITrend {
    RISING_WITH_PRICE,   // Bullish confirmation
    FALLING_WITH_PRICE,  // Bearish confirmation
    RISING_PRICE_FALLING, // Bearish divergence
    FALLING_PRICE_RISING  // Bullish divergence
}

data class LiquidationData(
    val symbol: String,
    val longLiquidations24h: Double,    // USD
    val shortLiquidations24h: Double,   // USD
    val dominantSide: String,           // "LONGS" или "SHORTS"
    val heatmapLevels: List<LiquidationLevel>,
    val timestamp: Long
)

data class LiquidationLevel(
    val price: Double,
    val longLiquidationUsd: Double,
    val shortLiquidationUsd: Double,
    val isSignificant: Boolean  // > $100M
)

data class MacroData(
    val sp500Price: Double,
    val sp500Change: Double,
    val goldPrice: Double,
    val goldChange: Double,
    val dxyPrice: Double,
    val dxyChange: Double,
    val btcSp500Correlation: Double,  // -1.0 до 1.0
    val btcGoldCorrelation: Double,
    val timestamp: Long
)

data class CalendarEvent(
    val id: Int,
    val title: String,
    val coins: List<String>,
    val dateEvent: String,
    val hotScore: Double,
    val isHot: Boolean,
    val category: String,
    val proofUrl: String?,
    val daysUntil: Int
)
