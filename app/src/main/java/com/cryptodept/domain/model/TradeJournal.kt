package com.cryptodept.domain.model

data class TradeJournal(
    val id: String,
    val coinId: String,
    val symbol: String,
    val direction: TradeDirection,
    val entryPrice: Double,
    val exitPrice: Double?,
    val quantity: Double,
    val entryTime: Long,
    val exitTime: Long?,
    val riskPercent: Double,
    val stopLoss: Double?,
    val takeProfit: Double?,
    val notes: String,
    val status: TradeStatus,
    val pnlUsd: Double?,
    val pnlPercent: Double?,
    val riskRewardActual: Double?,
    val positionSizeUsd: Double?,
    val marketConditions: String,
)

enum class TradeDirection { LONG, SHORT }

enum class TradeStatus { OPEN, CLOSED_WIN, CLOSED_LOSS }

data class JournalStats(
    val averagePnL: Double,
    val winRate: Double,
    val totalTrades: Int,
    val averageRR: Double,
)
