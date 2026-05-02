package com.cryptodept.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.data.api.GeminiCoachService
import com.cryptodept.domain.repository.JournalRepository
import com.cryptodept.domain.repository.CryptoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AICoachViewModel @Inject constructor(
    private val geminiService: GeminiCoachService,
    private val journalRepository: JournalRepository,
    private val cryptoRepository: CryptoRepository
) : ViewModel() {

    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        _messages.add(ChatMessage("COACH", "GREETINGS TRADER. I AM YOUR AI COACH. HOW CAN I ASSIST YOUR TERMINAL OPERATIONS TODAY?"))
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        
        _messages.add(ChatMessage("YOU", text))
        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                var responseText = ""
                _messages.add(ChatMessage("COACH", "..."))
                val lastIdx = _messages.size - 1
                
                geminiService.sendMessage(text).collect { chunk ->
                    responseText += chunk
                    _messages[lastIdx] = ChatMessage("COACH", responseText)
                }
            } catch (e: Exception) {
                _messages.add(ChatMessage("COACH", "[ERROR] SIGNAL LOST: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun analyzeJournal() {
        _isLoading.value = true
        _messages.add(ChatMessage("YOU", "ANALYZE MY TRADING JOURNAL"))
        
        viewModelScope.launch {
            try {
                val trades = journalRepository.getAllTrades().first()
                // Convert domain to entity for service (simplified)
                val entities = trades.map { com.cryptodept.data.db.TradeJournalEntity(
                    id = it.id, symbol = it.symbol, coinId = it.coinId, direction = it.direction.name,
                    entryPrice = it.entryPrice, quantity = it.quantity, entryTime = it.entryTime,
                    status = it.status.name, pnlPercent = it.pnlPercent, riskRewardActual = it.riskRewardActual,
                    exitPrice = it.exitPrice, exitTime = it.exitTime, riskPercent = it.riskPercent,
                    stopLoss = it.stopLoss, takeProfit = it.takeProfit, notes = it.notes,
                    pnlUsd = it.pnlUsd, positionSizeUsd = it.positionSizeUsd, marketConditions = it.marketConditions
                ) }
                
                var responseText = ""
                _messages.add(ChatMessage("COACH", "SCANNING DATABASE..."))
                val lastIdx = _messages.size - 1

                geminiService.analyzeJournal(entities, 50, "BULLISH").collect { chunk ->
                    responseText += chunk
                    _messages[lastIdx] = ChatMessage("COACH", responseText)
                }
            } catch (e: Exception) {
                _messages.add(ChatMessage("COACH", "[ERROR] DATA CORRUPTION: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }
}

data class ChatMessage(val sender: String, val text: String)
