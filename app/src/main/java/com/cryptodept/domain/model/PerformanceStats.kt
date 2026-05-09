package com.cryptodept.domain.model

data class PerformanceStats(
    val winRate: Double,
    val lossRate: Double,
    val profitFactor: Double,
    val averageWin: Double,
    val averageLoss: Double,
    val maxDrawdown: Double,
    val totalPnL: Double,
    val totalTrades: Int,
    val winningTrades: Int,
    val losingTrades: Int,
    val equityCurve: List<Double>,
    val averageTradeDuration: Long, // in ms
)

data class PerformanceReport(
    val stats: PerformanceStats,
    val aiInsights: String, // To be filled in Part B via Gemini
)
