package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.data.db.TradeJournalDao
import com.cryptodept.data.db.TradeJournalEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val journalDao: TradeJournalDao
) : ViewModel() {

    val allTrades: StateFlow<List<TradeJournalEntity>> =
        journalDao.getAllTrades()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<JournalStats> = allTrades.map { trades ->
        val closed = trades.filter { it.status != "OPEN" }
        val wins = closed.count { it.status == "CLOSED_WIN" }
        val winRate = if (closed.isEmpty()) 0f else wins.toFloat() / closed.size
        val avgRR = if (closed.isEmpty()) 0.0 else closed.mapNotNull { it.riskRewardActual }.average().let {
            if (it.isNaN()) 0.0 else it
        }
        val totalPnL = closed.mapNotNull { it.pnlUsd }.sum()
        JournalStats(winRate, avgRR, totalPnL, closed.size, trades.count { it.status == "OPEN" })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), JournalStats())

    fun addTrade(trade: TradeJournalEntity) {
        viewModelScope.launch { journalDao.insertTrade(trade) }
    }

    fun closeTrade(trade: TradeJournalEntity, exitPrice: Double) {
        viewModelScope.launch {
            val pnlPercent = if (trade.direction == "LONG") {
                (exitPrice - trade.entryPrice) / trade.entryPrice * 100
            } else {
                (trade.entryPrice - exitPrice) / trade.entryPrice * 100
            }
            val pnlUsd = pnlPercent / 100 * trade.entryPrice * trade.quantity
            val updated = trade.copy(
                exitPrice = exitPrice,
                exitTime = System.currentTimeMillis(),
                status = if (pnlPercent > 0) "CLOSED_WIN" else "CLOSED_LOSS",
                pnlPercent = pnlPercent,
                pnlUsd = pnlUsd
            )
            journalDao.updateTrade(updated)
        }
    }
}

data class JournalStats(
    val winRate: Float = 0f,
    val avgRR: Double = 0.0,
    val totalPnLUsd: Double = 0.0,
    val totalTrades: Int = 0,
    val openTrades: Int = 0
)
