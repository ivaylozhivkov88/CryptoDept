package com.cryptodept.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trade_journal")
data class TradeJournalEntity(
    @PrimaryKey val id: String, // UUID
    val coinId: String,
    val symbol: String,
    val direction: String, // "LONG" or "SHORT"
    val entryPrice: Double,
    val exitPrice: Double?, // null if position is open
    val quantity: Double,
    val entryTime: Long,
    val exitTime: Long?,
    val riskPercent: Double, // % от капитала рискуван
    val stopLoss: Double?,
    val takeProfit: Double?,
    val notes: String,
    val status: String, // "OPEN", "CLOSED_WIN", "CLOSED_LOSS"
    val pnlUsd: Double?, // Реализирана P&L
    val pnlPercent: Double?,
    val riskRewardActual: Double?, // Реализирано R:R
    val positionSizeUsd: Double?, // Добавено за Psychology Analyzer
    val marketConditions: String, // JSON: RSI, funding rate при отваряне
)
