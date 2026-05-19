package com.cryptodept.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class BacktesterEngine
    @Inject
    constructor(
        private val taEngine: TechnicalAnalysisEngine,
    ) {
        data class SimulatedTrade(
            val entryTimestamp: Long,
            val entryPrice: Double,
            val exitTimestamp: Long,
            val exitPrice: Double,
            val pnlPercent: Double,
            val pnlUsd: Double,
            val durationMs: Long,
        )

        data class BacktestConfig(
            val coinId: String,
            val startDate: Long,
            val endDate: Long,
            val rsiEntryThreshold: Double = 35.0,
            val rsiExitThreshold: Double = 65.0,
            val stopLossPercent: Double = 5.0,
            val takeProfitPercent: Double = 10.0,
            val initialCapital: Double = 10000.0,
            val riskPerTradePercent: Double = 2.0,
        )

        data class BacktestResult(
            val totalReturn: Double,
            val totalReturnUsd: Double,
            val winRate: Double,
            val totalTrades: Int,
            val maxDrawdown: Double,
            val sharpeRatio: Double,
            val profitFactor: Double,
            val trades: List<SimulatedTrade>,
            val equityCurve: List<Pair<Long, Double>>,
        )

        suspend fun run(
            config: BacktestConfig,
            ohlcData: List<com.cryptodept.domain.model.OHLCData>,
        ): BacktestResult =
            withContext(Dispatchers.Default) {
                val trades = mutableListOf<SimulatedTrade>()
                val equityCurve = mutableListOf<Pair<Long, Double>>()
                var currentCapital = config.initialCapital
                var peakCapital = config.initialCapital
                var maxDrawdown = 0.0

                if (ohlcData.isEmpty()) {
                    return@withContext BacktestResult(0.0, 0.0, 0.0, 0, 0.0, 0.0, 0.0, emptyList(), emptyList())
                }

                val prices = ohlcData.map { it.close }
                val timestamps = ohlcData.map { it.timestamp }

                var inPosition = false
                var entryPrice = 0.0
                var entryTime = 0L
                var positionSizeUsd = 0.0

                equityCurve.add(timestamps.first() to currentCapital)

                // We need at least 14 candles before we can start calculating RSI
                for (i in 14 until prices.size) {
                    val currentPrice = prices[i]
                    val currentTime = timestamps[i]

                    // 1. Calculate RSI for a smaller window to ensure we get data
                    val window = prices.subList(maxOf(0, i - 14), i + 1)
                    val rsi = if (window.size >= 14) taEngine.calculateRSI(window) else 50.0

                    if (!inPosition) {
                        // ENTRY LOGIC: RSI Oversold (Inclusive boundary)
                        if (rsi <= config.rsiEntryThreshold) {
                            inPosition = true
                            entryPrice = currentPrice
                            entryTime = currentTime
                            
                            val amountToRisk = currentCapital * (config.riskPerTradePercent / 100.0)
                            positionSizeUsd = if (config.stopLossPercent > 0) {
                                amountToRisk / (config.stopLossPercent / 100.0)
                            } else {
                                currentCapital * 0.5
                            }
                            positionSizeUsd = positionSizeUsd.coerceIn(currentCapital * 0.05, currentCapital)
                        }
                    } else {
                        // EXIT LOGIC
                        val pnlPct = (currentPrice - entryPrice) / entryPrice * 100.0
                        val isStopLoss = pnlPct <= -config.stopLossPercent
                        val isTakeProfit = pnlPct >= config.takeProfitPercent
                        val isRsiExit = rsi >= config.rsiExitThreshold

                        if (isStopLoss || isTakeProfit || isRsiExit) {
                            // Close trade
                            val actualPnlPct = when {
                                isStopLoss -> -config.stopLossPercent
                                isTakeProfit -> config.takeProfitPercent
                                else -> pnlPct
                            }
                            val pnlUsd = positionSizeUsd * (actualPnlPct / 100.0)
                            currentCapital += pnlUsd

                            trades.add(
                                SimulatedTrade(
                                    entryTimestamp = entryTime,
                                    entryPrice = entryPrice,
                                    exitTimestamp = currentTime,
                                    exitPrice = currentPrice,
                                    pnlPercent = actualPnlPct,
                                    pnlUsd = pnlUsd,
                                    durationMs = currentTime - entryTime,
                                ),
                            )

                            inPosition = false
                            peakCapital = maxOf(peakCapital, currentCapital)
                            val drawdown = (peakCapital - currentCapital) / peakCapital * 100.0
                            maxDrawdown = maxOf(maxDrawdown, drawdown)
                        }
                    }
                    equityCurve.add(currentTime to currentCapital)
                }

                // Finalize results
                val totalReturnUsd = currentCapital - config.initialCapital
                val totalReturnPct = (totalReturnUsd / config.initialCapital) * 100.0
                val wins = trades.count { it.pnlUsd > 0 }

                // PRICHINA 2: Division by zero fixes
                val winRate =
                    if (trades.isEmpty()) {
                        0.0
                    } else {
                        (wins.toDouble() / trades.size.toDouble()) * 100.0
                    }

                val grossProfit = trades.filter { it.pnlUsd > 0 }.sumOf { it.pnlUsd }
                val grossLoss = Math.abs(trades.filter { it.pnlUsd < 0 }.sumOf { it.pnlUsd })
                val profitFactor =
                    if (grossLoss <= 0.0) {
                        grossProfit
                    } else {
                        grossProfit / grossLoss
                    }

                // Simple Sharpe Ratio (daily frequency assumed, simplified)
                val returns = trades.map { it.pnlPercent / 100.0 }
                val avgReturn = if (returns.isNotEmpty()) returns.average() else 0.0
                val stdDev =
                    if (returns.size > 1) {
                        sqrt(returns.map { (it - avgReturn) * (it - avgReturn) }.average())
                    } else {
                        0.0
                    }

                val riskFreeRate = 0.02 / 365.0 // Simplified daily risk free rate
                val sharpeRatio =
                    if (stdDev <= 0.0) {
                        0.0
                    } else {
                        ((avgReturn - riskFreeRate) / stdDev) * sqrt(252.0) // Annualized
                    }

                BacktestResult(
                    totalReturn = totalReturnPct,
                    totalReturnUsd = totalReturnUsd,
                    winRate = winRate,
                    totalTrades = trades.size,
                    maxDrawdown = maxDrawdown,
                    sharpeRatio = sharpeRatio,
                    profitFactor = profitFactor,
                    trades = trades,
                    equityCurve = equityCurve,
                )
            }
    }
