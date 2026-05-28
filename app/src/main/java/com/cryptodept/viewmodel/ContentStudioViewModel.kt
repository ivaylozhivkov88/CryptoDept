package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.data.content.PromptTemplates
import com.cryptodept.data.remote.source.FirebaseRemoteDataSource
import com.cryptodept.domain.repository.AIProvider
import com.cryptodept.domain.repository.CryptoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContentStudioViewModel @Inject constructor(
    private val aiProvider: AIProvider,
    private val repository: CryptoRepository,
    private val firebaseDataSource: FirebaseRemoteDataSource
) : ViewModel() {
    private val _uiState = MutableStateFlow(ContentStudioUiState())
    val uiState: StateFlow<ContentStudioUiState> = _uiState.asStateFlow()

    private val _favorites = MutableStateFlow<List<com.cryptodept.domain.model.CoinPrice>>(emptyList())
    val favorites: StateFlow<List<com.cryptodept.domain.model.CoinPrice>> = _favorites.asStateFlow()

    val cloudState = firebaseDataSource.getTerminalState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        loadFavorites()
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            repository.getTrackedCoinPrices().collect { coins ->
                _favorites.value = coins
            }
        }
    }

    fun setScope(isGlobal: Boolean) {
        _uiState.update { it.copy(isGlobalScope = isGlobal) }
    }

    fun setSelectedCoin(coinId: String) {
        _uiState.update { it.copy(selectedCoinId = coinId) }
    }

    fun generateContent(type: ContentCategory) {
        val isGlobal = _uiState.value.isGlobalScope
        val scopeName = if (isGlobal) "GLOBAL_MARKET" else _uiState.value.selectedCoinId.uppercase()
        
        // --- 1. EXTRACT REAL MARKET CONTEXT ---
        val cloud = cloudState.value
        val contextData = if (isGlobal) {
            val fg = cloud?.macroBriefing?.fearGreedIndex ?: 50
            val risk = cloud?.macroBriefing?.riskScore ?: 50
            "FearGreed: $fg, RiskScore: $risk, GlobalLiquidity: ${cloud?.macroBriefing?.globalLiquidityUsd ?: "SYNCING"}"
        } else {
            val coinData = cloud?.marketData?.get(_uiState.value.selectedCoinId)
            "Price: $${coinData?.currentPrice}, RSI: ${coinData?.rsi}, Trend: ${coinData?.trend}, Risk: ${coinData?.riskScore}"
        }

        val prompt = when (type) {
            ContentCategory.TEXT -> PromptTemplates.buildSocialPostPrompt(scopeName, contextData)
            ContentCategory.CHART -> PromptTemplates.buildInfographicPrompt(scopeName, contextData)
            ContentCategory.VIDEO -> PromptTemplates.buildCinematicVideoPrompt(scopeName, contextData)
        }
        
        _uiState.update { it.copy(isLoading = true, lastGeneratedType = type, generatedOutput = "") }
        
        viewModelScope.launch {
            try {
                var fullResponse = ""
                aiProvider.sendMessage(prompt).collect { chunk ->
                    fullResponse += chunk
                    _uiState.update { it.copy(generatedOutput = fullResponse) }
                }
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun navigationConsumed() {
        _uiState.update { it.copy(pendingNavigationToAiCoach = null) }
    }

    fun sendPromptToAiCoach(prompt: String) {
        _uiState.update { it.copy(pendingNavigationToAiCoach = prompt) }
    }
}

enum class ContentCategory {
    TEXT, CHART, VIDEO
}

data class ContentStudioUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isGlobalScope: Boolean = true,
    val selectedCoinId: String = "bitcoin",
    val generatedOutput: String = "",
    val lastGeneratedType: ContentCategory? = null,
    val pendingNavigationToAiCoach: String? = null,
)
