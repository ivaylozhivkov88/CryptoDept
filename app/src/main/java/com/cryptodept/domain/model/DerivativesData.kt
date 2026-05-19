package com.cryptodept.domain.model

data class FundingRateData(
    val symbol: String,
    val markPrice: Double,
    val binanceRate: Double,
    val aggregatedRate: Double,
    val nextFundingTime: Long,
    val rateLevel: FundingLevel,
    val timestamp: Long,
)

enum class FundingLevel(
    val description: String,
    val isBullishWarning: Boolean,
) {
    VERY_HIGH("Extreme Bullish Sentiment — Crash Risk", true),
    HIGH("Elevated Bullish Sentiment — Caution", true),
    NORMAL("Neutral Funding — Healthy Market", false),
    LOW("Slight Bearish Sentiment", false),
    VERY_LOW("Extreme Bearish Sentiment — Bounce Risk", false),
}

data class OpenInterestData(
    val symbol: String,
    val openInterestUsd: Double,
    val openInterestChange24h: Double,
    val trend: OITrend,
    val history: List<OHLCData>,
    val timestamp: Long,
)

enum class OITrend {
    RISING_WITH_PRICE,
    FALLING_WITH_PRICE,
    RISING_PRICE_FALLING,
    FALLING_PRICE_RISING,
}

data class LiquidationData(
    val symbol: String,
    val longLiquidations24h: Double,
    val shortLiquidations24h: Double,
    val dominantSide: String,
    val heatmapLevels: List<LiquidationLevel>,
    val timestamp: Long,
)

data class LiquidationLevel(
    val price: Double,
    val longLiquidationUsd: Double,
    val shortLiquidationUsd: Double,
    val isSignificant: Boolean,
)

data class MacroData(
    val sp500Price: Double,
    val sp500Change: Double,
    val goldPrice: Double,
    val goldChange: Double,
    val dxyPrice: Double,
    val dxyChange: Double,
    val btcSp500Correlation: Double,
    val btcGoldCorrelation: Double,
    val timestamp: Long,
)

data class FundingHeatmapItem(
    val symbol: String,
    val binanceRate: Double,
    val bybitRate: Double,
    val okxRate: Double,
    val averageRate: Double,
)

data class MagneticZone(
    val price: Double,
    val totalLiquidationUsd: Double,
    val distancePercent: Double,
    val type: LiquidationType,
)

enum class LiquidationType {
    LONG_SQUEEZE_POTENTIAL,
    SHORT_SQUEEZE_POTENTIAL
}

data class CalendarEvent(
    val id: Int,
    val title: String,
    val coins: List<String>,
    val dateEvent: String,
    val hotScore: Double,
    val isHot: Boolean,
    val category: String,
    val daysUntil: Int,
)

// NEW WRAPPER FOR PART 5
data class DerivativesSnapshot(
    val coinId: String,
    val funding: FundingRateData?,
    val openInterest: OpenInterestData?,
    val liquidations: LiquidationData?,
)
