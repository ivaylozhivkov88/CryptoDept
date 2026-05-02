package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.model.*
import com.cryptodept.domain.repository.JournalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val repository: JournalRepository,
    private val analytics: com.cryptodept.util.AnalyticsManager
) : ViewModel() {

    sealed class JournalUiState {
        object Loading : JournalUiState()
        data class Success(val trades: List<TradeJournal>) : JournalUiState()
        data class Error(val message: String) : JournalUiState()
    }

    private val _uiState = MutableStateFlow<JournalUiState>(JournalUiState.Loading)
    val uiState: StateFlow<JournalUiState> = _uiState.asStateFlow()

    init {
        repository.getAllTrades()
            .onEach { trades -> _uiState.value = JournalUiState.Success(trades) }
            .catch { _uiState.value = JournalUiState.Error(it.message ?: "DATABASE ERROR") }
            .launchIn(viewModelScope)
    }

    val allTrades: StateFlow<List<TradeJournal>> = uiState.map { 
        if (it is JournalUiState.Success) it.trades else emptyList() 
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val journalStats: StateFlow<JournalStats> = allTrades.map { trades ->
        val closed = trades.filter { it.status != TradeStatus.OPEN }
        val wins = closed.count { it.status == TradeStatus.CLOSED_WIN }
        val winRate = if (closed.isEmpty()) 0.0 else (wins.toDouble() / closed.size) * 100
        val avgRR = if (closed.isEmpty()) 0.0 else closed.mapNotNull { it.riskRewardActual }.average().let {
            if (it.isNaN()) 0.0 else it
        }
        val totalPnL = closed.mapNotNull { it.pnlUsd }.sum()
        JournalStats(totalPnL, winRate, closed.size, avgRR)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), JournalStats(0.0, 0.0, 0, 0.0))

    fun addTrade(trade: TradeJournal) {
        analytics.logTradeLogged(trade.direction.name, "OPEN")
        viewModelScope.launch(Dispatchers.IO) { repository.addTrade(trade) }
    }

    fun deleteTrade(trade: TradeJournal) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteTrade(trade) }
    }

    fun closeTrade(trade: TradeJournal, exitPrice: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val pnlPercent = if (trade.direction == TradeDirection.LONG) {
                (exitPrice - trade.entryPrice) / trade.entryPrice * 100
            } else {
                (trade.entryPrice - exitPrice) / trade.entryPrice * 100
            }
            val pnlUsd = pnlPercent / 100 * trade.entryPrice * trade.quantity
            
            val riskReward = if (trade.stopLoss != null && trade.stopLoss != trade.entryPrice) {
                val risk = Math.abs(trade.entryPrice - trade.stopLoss)
                val reward = Math.abs(exitPrice - trade.entryPrice)
                reward / risk
            } else null

            val updated = trade.copy(
                exitPrice = exitPrice,
                exitTime = System.currentTimeMillis(),
                status = if (pnlPercent > 0) TradeStatus.CLOSED_WIN else TradeStatus.CLOSED_LOSS,
                pnlPercent = pnlPercent,
                pnlUsd = pnlUsd,
                riskRewardActual = riskReward
            )
            analytics.logTradeLogged(trade.direction.name, updated.status.name)
            repository.updateTrade(updated)
        }
    }
}
