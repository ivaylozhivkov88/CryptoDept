package com.cryptodept.domain.model

data class TechnicalIndicators(
    val rsi: Float,
    val macd: MACDData,
    val bollingerBands: BollingerBandsData,
    val emas: Map<Int, Double>,
    val trend: TrendSignal,
    val supportLevels: List<Double>,
    val resistanceLevels: List<Double>
) {
    companion object {
        fun default() = TechnicalIndicators(
            rsi = 50f,
            macd = MACDData(0f, 0f, 0f),
            bollingerBands = BollingerBandsData(0.0, 0.0, 0.0),
            emas = emptyMap(),
            trend = TrendSignal.NEUTRAL,
            supportLevels = emptyList(),
            resistanceLevels = emptyList()
        )
    }
}

data class MACDData(
    val macdLine: Float,
    val signalLine: Float,
    val histogram: Float
)

data class BollingerBandsData(
    val upper: Double,
    val middle: Double,
    val lower: Double
)

enum class TrendSignal {
    STRONG_BULLISH, BULLISH, NEUTRAL, BEARISH, STRONG_BEARISH
}