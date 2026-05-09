package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.PerformanceStats
import com.cryptodept.domain.model.TradeStatus
import com.cryptodept.domain.repository.JournalRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class CalculatePerformanceUseCase
    @Inject
    constructor(
        private val repository: JournalRepository,
    ) {
        suspend operator fun invoke(): Result<PerformanceStats> {
            return try {
                val trades =
                    repository
                        .getAllTrades()
                        .first()
                        .filter { it.status != TradeStatus.OPEN } // Only closed trades

                if (trades.isEmpty()) return Result.failure(Exception("NO_CLOSED_TRADES"))

                val winningTrades = trades.filter { (it.pnlUsd ?: 0.0) > 0 }
                val losingTrades = trades.filter { (it.pnlUsd ?: 0.0) <= 0 }

                val totalPnL = trades.sumOf { it.pnlUsd ?: 0.0 }
                val winRate = winningTrades.size.toDouble() / trades.size

                val avgWin = if (winningTrades.isNotEmpty()) winningTrades.map { it.pnlUsd ?: 0.0 }.average() else 0.0
                val avgLoss = if (losingTrades.isNotEmpty()) losingTrades.map { abs(it.pnlUsd ?: 0.0) }.average() else 0.0

                val totalProfit = winningTrades.sumOf { it.pnlUsd ?: 0.0 }
                val totalLoss = losingTrades.sumOf { abs(it.pnlUsd ?: 0.0) }
                val profitFactor = if (totalLoss > 0) totalProfit / totalLoss else totalProfit

                // Build Equity Curve
                var currentEquity = 0.0
                val equityCurve = mutableListOf<Double>()
                equityCurve.add(0.0)
                trades.sortedBy { it.exitTime }.forEach {
                    currentEquity += (it.pnlUsd ?: 0.0)
                    equityCurve.add(currentEquity)
                }

                // Simple Max Drawdown
                var maxEquity = 0.0
                var maxDD = 0.0
                equityCurve.forEach { equity ->
                    if (equity > maxEquity) maxEquity = equity
                    val dd = maxEquity - equity
                    if (dd > maxDD) maxDD = dd
                }

                val avgDuration =
                    trades
                        .mapNotNull { t ->
                            if (t.exitTime != null) t.exitTime - t.entryTime else null
                        }.average()
                        .toLong()

                Result.success(
                    PerformanceStats(
                        winRate = winRate,
                        lossRate = 1.0 - winRate,
                        profitFactor = profitFactor,
                        averageWin = avgWin,
                        averageLoss = avgLoss,
                        maxDrawdown = maxDD,
                        totalPnL = totalPnL,
                        totalTrades = trades.size,
                        winningTrades = winningTrades.size,
                        losingTrades = losingTrades.size,
                        equityCurve = equityCurve,
                        averageTradeDuration = avgDuration,
                    ),
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
