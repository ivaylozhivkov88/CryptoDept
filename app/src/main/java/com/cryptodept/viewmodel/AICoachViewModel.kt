package com.cryptodept.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.repository.AIProvider
import com.cryptodept.domain.repository.CryptoRepository
import com.cryptodept.domain.repository.JournalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AICoachViewModel
    @Inject
    constructor(
        private val aiProvider: AIProvider,
        private val journalRepository: JournalRepository,
        private val cryptoRepository: CryptoRepository,
        private val riskEngine: com.cryptodept.domain.usecase.RiskScoreEngine,
    ) : ViewModel() {
        private val _messages = mutableStateListOf<ChatMessage>()
        val messages: List<ChatMessage> = _messages

        private val _isLoading = MutableStateFlow(false)
        val isLoading: StateFlow<Boolean> = _isLoading

        private val _inputText = MutableStateFlow("")
        val inputText: StateFlow<String> = _inputText

        init {
            _messages.add(ChatMessage("COACH", "GREETINGS TRADER. I AM YOUR AI COACH. HOW CAN I ASSIST YOUR TERMINAL OPERATIONS TODAY?"))
        }

        fun setInputText(text: String) {
            _inputText.value = text
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

                    aiProvider.sendMessage(text).collect { chunk ->
                        responseText += chunk
                        _messages[lastIdx] = ChatMessage("COACH", responseText)
                    }
                } catch (e: Exception) {
                    val errorMsg = if (e.message?.contains("blocked") == true) {
                        "ACCESS_DENIED: Application identity verification failed. Ensure API key restrictions include this package name and SHA-1 certificate."
                    } else {
                        "SIGNAL LOST: ${e.message}"
                    }
                    _messages.add(ChatMessage("COACH", "[ERROR] $errorMsg"))
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
                    val btcChange = cryptoRepository.getCachedChange24h("bitcoin")
                    val marketTrend = if (btcChange > 0.5) "BULLISH" else if (btcChange < -0.5) "BEARISH" else "NEUTRAL"
                    val currentRisk = riskEngine.currentScore.value

                    var responseText = ""
                    _messages.add(ChatMessage("COACH", "SCANNING DATABASE..."))
                    val lastIdx = _messages.size - 1

                    aiProvider.analyzeJournal(trades, currentRisk, marketTrend).collect { chunk ->
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

data class ChatMessage(
    val sender: String,
    val text: String,
)
