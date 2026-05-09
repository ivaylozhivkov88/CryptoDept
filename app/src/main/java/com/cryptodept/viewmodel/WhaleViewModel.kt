package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.data.api.UnifiedWebSocketManager
import com.cryptodept.domain.model.Blockchain
import com.cryptodept.domain.model.MarketEvent
import com.cryptodept.domain.model.WhaleTransaction
import com.cryptodept.domain.repository.WhaleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class WhaleViewModel
    @Inject
    constructor(
        private val whaleRepository: WhaleRepository,
        private val wsManager: UnifiedWebSocketManager,
    ) : ViewModel() {
        private val _isRefreshing = MutableStateFlow(false)
        val isRefreshing = _isRefreshing.asStateFlow()

        private val _wsTransactions = MutableStateFlow<List<WhaleTransaction>>(emptyList())

        val transactions: StateFlow<List<WhaleTransaction>> =
            combine(
                whaleRepository.getWhaleTransactions(),
                _wsTransactions,
            ) { dbList, wsList ->
                (wsList + dbList).sortedByDescending { it.timestamp }.take(50)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        init {
            refresh()
            observeWebSockets()
        }

        private fun observeWebSockets() {
            viewModelScope.launch {
                wsManager.marketEvents
                    .filterIsInstance<MarketEvent.LargeTrade>()
                    .collect { trade ->
                        val whaleTx = WhaleTransaction(
                            id = UUID.randomUUID().toString(),
                            symbol = trade.symbol.replace("USDT", ""),
                            amount = trade.quantity,
                            amountUsd = trade.amountUsd,
                            fromAddress = "Exchange (WS)",
                            toAddress = if (trade.side == "BUY") "Whale Wallet" else "Exchange",
                            blockchain = Blockchain.ETHEREUM, // Simplified
                            transactionHash = "ws_${trade.timestamp}",
                            timestamp = trade.timestamp
                        )
                        _wsTransactions.value = (listOf(whaleTx) + _wsTransactions.value).take(10)
                    }
            }
        }

        fun refresh() {
            viewModelScope.launch {
                _isRefreshing.value = true
                whaleRepository.refreshWhaleTransactions()
                _isRefreshing.value = false
            }
        }
    }
