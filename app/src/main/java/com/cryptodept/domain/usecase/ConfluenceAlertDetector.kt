package com.cryptodept.domain.usecase

import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfluenceAlertDetector @Inject constructor() {

    data class ConfluenceAlert(
        val id: String,
        val coin: String,
        val type: ConfluenceType,
        val signals: List<String>,       // Кои сигнали се съгласуват
        val signalCount: Int,
        val strength: Float,             // 0.0 - 1.0
        val direction: String,           // "BULLISH" или "BEARISH"
        val suggestedAction: String,
        val timestamp: Long,
        val price: Double
    )

    enum class ConfluenceType(val minSignals: Int, val label: String) {
        WEAK(2, "WEAK CONFLUENCE"),
        MODERATE(3, "CONFLUENCE ALERT"),
        STRONG(4, "STRONG CONFLUENCE"),
        EXTREME(5, "EXTREME CONFLUENCE — HIGH PROBABILITY")
    }

    fun detect(
        coin: String,
        price: Double,
        rsi: Double,
        macdBullish: Boolean,
        priceAboveEma50: Boolean,
        priceAboveEma200: Boolean,
        fundingRate: Double,
        fearGreedIndex: Int,
        bollingerPosition: Double,  // -1.0 (at lower) до +1.0 (at upper)
        exchangeInflowChange: Double
    ): ConfluenceAlert? {

        val bullishSignals = mutableListOf<String>()
        val bearishSignals = mutableListOf<String>()

        // RSI
        if (rsi < 30) bullishSignals.add("RSI OVERSOLD (${String.format(Locale.ENGLISH, "%.1f", rsi)})")
        if (rsi > 70) bearishSignals.add("RSI OVERBOUGHT (${String.format(Locale.ENGLISH, "%.1f", rsi)})")

        // MACD
        if (macdBullish) bullishSignals.add("MACD BULLISH CROSSOVER")
        else bearishSignals.add("MACD BEARISH CROSSOVER")

        // EMA
        if (priceAboveEma50) bullishSignals.add("PRICE ABOVE EMA50")
        else bearishSignals.add("PRICE BELOW EMA50")

        if (priceAboveEma200) bullishSignals.add("PRICE ABOVE EMA200 (BULL MARKET)")
        else bearishSignals.add("PRICE BELOW EMA200 (BEAR MARKET)")

        // Funding Rate (contrarian — high funding = bearish для LONG позиции)
        if (fundingRate < -0.02) bullishSignals.add("NEGATIVE FUNDING (shorts overlevered)")
        if (fundingRate > 0.08) bearishSignals.add("EXTREME FUNDING (longs overlevered)")

        // Fear & Greed (contrarian)
        if (fearGreedIndex < 25) bullishSignals.add("EXTREME FEAR (contrarian buy signal)")
        if (fearGreedIndex > 80) bearishSignals.add("EXTREME GREED (contrarian sell signal)")

        // Bollinger Bands
        if (bollingerPosition < -0.8) bullishSignals.add("PRICE AT LOWER BOLLINGER BAND")
        if (bollingerPosition > 0.8) bearishSignals.add("PRICE AT UPPER BOLLINGER BAND")

        // Exchange Inflows (contrarian)
        if (exchangeInflowChange < -20) bullishSignals.add("EXCHANGE OUTFLOWS (accumulation)")
        if (exchangeInflowChange > 30) bearishSignals.add("EXCHANGE INFLOW SPIKE (sell pressure)")

        // Определи посоката
        val isBullish = bullishSignals.size > bearishSignals.size
        val dominantSignals = if (isBullish) bullishSignals else bearishSignals
        val signalCount = dominantSignals.size

        if (signalCount < ConfluenceType.WEAK.minSignals) return null

        val type = when {
            signalCount >= ConfluenceType.EXTREME.minSignals -> ConfluenceType.EXTREME
            signalCount >= ConfluenceType.STRONG.minSignals  -> ConfluenceType.STRONG
            signalCount >= ConfluenceType.MODERATE.minSignals -> ConfluenceType.MODERATE
            else -> ConfluenceType.WEAK
        }

        val direction = if (isBullish) "BULLISH" else "BEARISH"
        val action = when {
            isBullish && type == ConfluenceType.EXTREME -> "STRONG BUY SIGNAL — Multiple indicators aligned"
            isBullish && type == ConfluenceType.STRONG  -> "Consider long entry with tight stop loss"
            !isBullish && type == ConfluenceType.EXTREME -> "STRONG SELL SIGNAL — Consider reducing exposure"
            !isBullish && type == ConfluenceType.STRONG  -> "Consider taking profits or hedging"
            else -> "Monitor closely — confluence forming"
        }

        return ConfluenceAlert(
            id = "${coin}_${System.currentTimeMillis()}",
            coin = coin,
            type = type,
            signals = dominantSignals,
            signalCount = signalCount,
            strength = (signalCount.toFloat() / 9f).coerceIn(0f, 1f),
            direction = direction,
            suggestedAction = action,
            timestamp = System.currentTimeMillis(),
            price = price
        )
    }
}
