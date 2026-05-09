package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BacktestEngine @Inject constructor(
    private val strategyEngine: StrategyEngine
) {
    fun runBacktest(
        strategy: TradingStrategy,
        history: List<OHLCData>,
        initialCapital: Double = 1000.0
    ): BacktestReport {
        var capital = initialCapital
        var positionSize = 0.0
        var tradesCount = 0
        var wins = 0
        var maxDrawdown = 0.0
        var peakCapital = initialCapital

        val equityCurve = mutableListOf<Double>()

        history.forEach { candle ->
            val snapshot = MarketDataSnapshot(
                price = candle.close,
                rsi = 50.0, 
                macdSignal = "N/A",
                ema50Signal = "N/A",
                ema200Signal = "N/A",
                bollingerPosition = "N/A",
                fundingRate = 0.0,
                fundingLevel = "N/A",
                longLiquidations24h = 0.0,
                shortLiquidations24h = 0.0,
                fearGreedIndex = 50,
                newsSentiment = "NEUTRAL",
                wyckoffPhase = "N/A",
                elliottWave = "N/A",
                riskScore = 50,
                priceChange24h = 0.0,
                btcDominance = 50.0,
                sp500Change = 0.0,
                dxyChange = 0.0
            )

            if (positionSize == 0.0) {
                if (strategyEngine.evaluateEntry(strategy, snapshot)) {
                    // Buy
                    positionSize = capital / candle.close
                    capital = 0.0
                    tradesCount++
                }
            } else {
                if (strategyEngine.evaluateExit(strategy, snapshot)) {
                    // Sell
                    capital = positionSize * candle.close
                    if (capital > initialCapital) wins++
                    positionSize = 0.0
                    tradesCount++
                }
            }

            val currentEquity = if (positionSize > 0.0) positionSize * candle.close else capital
            equityCurve.add(currentEquity)

            if (currentEquity > peakCapital) peakCapital = currentEquity
            val dd = if (peakCapital > 0) (peakCapital - currentEquity) / peakCapital else 0.0
            if (dd > maxDrawdown) maxDrawdown = dd
        }

        val finalCapital = if (positionSize > 0.0) positionSize * (history.lastOrNull()?.close ?: 0.0) else capital

        return BacktestReport(
            totalReturnPercent = if (initialCapital > 0) ((finalCapital - initialCapital) / initialCapital) * 100 else 0.0,
            winRate = if (tradesCount > 0) (wins.toDouble() / tradesCount) * 100 else 0.0,
            tradesCount = tradesCount,
            maxDrawdownPercent = maxDrawdown * 100,
            finalBalance = finalCapital,
            equityCurve = equityCurve
        )
    }
}

data class BacktestReport(
    val totalReturnPercent: Double,
    val winRate: Double,
    val tradesCount: Int,
    val maxDrawdownPercent: Double,
    val finalBalance: Double,
    val equityCurve: List<Double>
)
