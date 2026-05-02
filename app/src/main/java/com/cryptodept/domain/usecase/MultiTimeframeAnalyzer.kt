package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.*
import com.cryptodept.domain.repository.ChartRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MultiTimeframeAnalyzer @Inject constructor(
    private val taEngine: TechnicalAnalysisEngine,
    private val chartRepository: ChartRepository
) {
    // Mapping: timeframe label → CoinGecko days param
    private val TIMEFRAME_CONFIG = listOf(
        Triple("15m", 1, 96),    // 1 ден данни, последните 96 свещи
        Triple("1H",  1, 24),    // 1 ден данни, последните 24 свещи
        Triple("4H",  7, 42),    // 7 дни данни, последните 42 свещи
        Triple("1D",  30, 30),   // 30 дни данни
        Triple("1W",  90, 13)    // 90 дни данни (приблизително 13 седмични свещи)
    )

    suspend fun analyze(coinId: String): MTFConsensus = withContext(Dispatchers.Default) {
        val timeframeSignals = mutableListOf<TimeframeSignal>()

        TIMEFRAME_CONFIG.forEach { (tfLabel, days, take) ->
            try {
                val allCandles = chartRepository.getOHLCData(coinId, days).first()
                val candles = allCandles.takeLast(take)
                if (candles.size < 14) return@forEach

                val prices = candles.map { it.close }
                val rsi = taEngine.calculateRSI(prices)
                val macd = taEngine.calculateMACD(prices)
                val ema20 = taEngine.calculateEMA(prices, 20).last()
                val ema50 = taEngine.calculateEMA(prices, minOf(50, prices.size)).last()
                val currentPrice = prices.last()

                val trend = determineTrend(prices, ema20, ema50)
                val macdSignal = determineMacdSignal(macd)
                val emaSignal = determineEmaSignal(currentPrice, ema20, ema50)
                val overall = calculateOverall(trend, rsi, macdSignal, emaSignal)

                timeframeSignals.add(TimeframeSignal(
                    tfLabel, trend, rsi, macdSignal, emaSignal, overall, candles.size
                ))
            } catch (e: Exception) {
                android.util.Log.e("CryptoDept_MTF", "Error for $tfLabel: ${e.message}")
            }
        }

        buildConsensus(timeframeSignals)
    }

    private fun determineTrend(prices: List<Double>, ema20: Double, ema50: Double): TrendDirection {
        val current = prices.last()
        val recentChange = if (prices.size >= 5) (current - prices[prices.size - 5]) / prices[prices.size - 5] * 100 else 0.0
        return when {
            current > ema20 && current > ema50 && recentChange > 2  -> TrendDirection.STRONG_UP
            current > ema20 && current > ema50                       -> TrendDirection.UP
            current < ema20 && current < ema50 && recentChange < -2  -> TrendDirection.STRONG_DOWN
            current < ema20 && current < ema50                       -> TrendDirection.DOWN
            else -> TrendDirection.SIDEWAYS
        }
    }

    private fun determineMacdSignal(macd: MACDResult): MacdSignal {
        val hist = macd.histogram
        if (hist.size < 2) return MacdSignal.NEUTRAL
        val current = hist.last()
        val previous = hist[hist.size - 2]
        return when {
            previous < 0 && current > 0 -> MacdSignal.BULLISH_CROSS
            previous > 0 && current < 0 -> MacdSignal.BEARISH_CROSS
            current > 0 && current > previous -> MacdSignal.BULLISH
            current < 0 && current < previous -> MacdSignal.BEARISH
            else -> MacdSignal.NEUTRAL
        }
    }

    private fun determineEmaSignal(price: Double, ema20: Double, ema50: Double): EmaSignal {
        return when {
            price > ema20 && price > ema50 && ema20 > ema50 -> EmaSignal.ABOVE_ALL
            price > ema50                                     -> EmaSignal.ABOVE_50
            price < ema20 && price < ema50 && ema20 < ema50 -> EmaSignal.BELOW_ALL
            price < ema50                                     -> EmaSignal.BELOW_50
            else -> EmaSignal.MIXED
        }
    }

    private fun calculateOverall(
        trend: TrendDirection, rsi: Double,
        macd: MacdSignal, ema: EmaSignal
    ): OverallSignal {
        var score = 0
        score += when (trend) {
            TrendDirection.STRONG_UP -> 2
            TrendDirection.UP -> 1
            TrendDirection.STRONG_DOWN -> -2
            TrendDirection.DOWN -> -1
            else -> 0
        }
        if (rsi < 35) score += 1
        if (rsi > 65) score -= 1
        score += when (macd) {
            MacdSignal.BULLISH_CROSS, MacdSignal.BULLISH -> 1
            MacdSignal.BEARISH_CROSS, MacdSignal.BEARISH -> -1
            else -> 0
        }
        score += when (ema) {
            EmaSignal.ABOVE_ALL -> 1
            EmaSignal.BELOW_ALL -> -1
            else -> 0
        }
        return when {
            score >= 4  -> OverallSignal.STRONG_BUY
            score >= 2  -> OverallSignal.BUY
            score <= -4 -> OverallSignal.STRONG_SELL
            score <= -2 -> OverallSignal.SELL
            else        -> OverallSignal.NEUTRAL
        }
    }

    private fun buildConsensus(signals: List<TimeframeSignal>): MTFConsensus {
        val bullish = signals.count { it.overallSignal in listOf(OverallSignal.BUY, OverallSignal.STRONG_BUY) }
        val bearish = signals.count { it.overallSignal in listOf(OverallSignal.SELL, OverallSignal.STRONG_SELL) }
        val neutral = signals.size - bullish - bearish

        val consensus = when {
            bullish >= 4 -> OverallSignal.STRONG_BUY
            bullish >= 3 -> OverallSignal.BUY
            bearish >= 4 -> OverallSignal.STRONG_SELL
            bearish >= 3 -> OverallSignal.SELL
            else         -> OverallSignal.NEUTRAL
        }

        val interpretation = when {
            bullish >= 4 -> "Strong alignment across all timeframes. Trend likely to continue."
            bullish == 3 && bearish == 0 -> "Higher timeframes bullish. Short-term weakness = dip opportunity."
            bearish >= 4 -> "Strong bearish alignment. Avoid longs."
            bullish > 0 && bearish > 0 -> "Mixed signals. Higher timeframes take priority. Wait for clarity."
            else -> "No clear direction. Market in consolidation."
        }

        val bias = when (consensus) {
            OverallSignal.STRONG_BUY, OverallSignal.BUY -> "LONG"
            OverallSignal.STRONG_SELL, OverallSignal.SELL -> "SHORT"
            else -> "WAIT"
        }

        return MTFConsensus(signals, bullish, bearish, neutral, consensus, interpretation, bias)
    }
}
