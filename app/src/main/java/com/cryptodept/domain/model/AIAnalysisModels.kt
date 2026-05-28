package com.cryptodept.domain.model

data class MarketDataSnapshot(
    val price: Double,
    val rsi: Double,
    val macdSignal: String,
    val ema50Signal: String,
    val ema200Signal: String,
    val bollingerPosition: String,
    val fundingRate: Double,
    val fundingLevel: String,
    val longLiquidations24h: Double,
    val shortLiquidations24h: Double,
    val fearGreedIndex: Int,
    val newsSentiment: String,
    val wyckoffPhase: String,
    val elliottWave: String,
    val riskScore: Int,
    val priceChange24h: Double,
    val btcDominance: Double,
    val sp500Change: Double,
    val dxyChange: Double,
    val ohlc: List<OHLCData> = emptyList(),
    val exchangeInflowUsd: Double = 0.0,
    val exchangeOutflowUsd: Double = 0.0,
)

data class ParsedReport(
    val tldr: String,
    val verdict: ReportVerdict,
    val sections: List<ReportSection>,
    val keyLevels: List<String>,
    val watchFor: List<String>,
    val disclaimer: String,
)

data class ReportSection(
    val title: String,
    val overview: String,
    val whatItMeans: String,
    val watchFor: String,
)

enum class ReportVerdict {
    STRONG_BULLISH,
    BULLISH,
    NEUTRAL,
    BEARISH,
    STRONG_BEARISH,
}
