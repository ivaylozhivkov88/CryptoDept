package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.model.PerformanceStats
import com.cryptodept.domain.repository.AIProvider
import com.cryptodept.domain.usecase.CalculatePerformanceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

sealed class PerformanceUiState {
    object Loading : PerformanceUiState()
    object Empty : PerformanceUiState()

    data class Success(
        val stats: PerformanceStats,
        val aiInsights: String,
    ) : PerformanceUiState()

    data class Error(
        val message: String,
    ) : PerformanceUiState()
}

@HiltViewModel
class PerformanceViewModel
    @Inject
    constructor(
        private val calculatePerformance: CalculatePerformanceUseCase,
        private val aiProvider: AIProvider,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<PerformanceUiState>(PerformanceUiState.Loading)
        val uiState: StateFlow<PerformanceUiState> = _uiState.asStateFlow()

        init {
            loadPerformance()
        }

        fun loadPerformance() {
            viewModelScope.launch {
                _uiState.value = PerformanceUiState.Loading
                kotlinx.coroutines.delay(500)
                calculatePerformance()
                    .onSuccess { stats ->
                        generateAiInsights(stats)
                    }.onFailure {
                        if (it.message == "NO_CLOSED_TRADES") {
                            _uiState.value = PerformanceUiState.Empty
                        } else {
                            _uiState.value = PerformanceUiState.Error(it.message ?: "STATS_CALCULATION_FAILED")
                        }
                    }
            }
        }

        private fun generateAiInsights(stats: PerformanceStats) {
            viewModelScope.launch {
                try {
                    val prompt =
                        """
                        Analyze my trading performance:
                        Win Rate: ${String.format(Locale.US, "%.1f", stats.winRate * 100)}%
                        Profit Factor: ${String.format(Locale.US, "%.2f", stats.profitFactor)}
                        Avg Win: ${String.format(Locale.US, "$%.2f", stats.averageWin)}
                        Avg Loss: ${String.format(Locale.US, "$%.2f", stats.averageLoss)}
                        Max Drawdown: ${String.format(Locale.US, "$%.2f", stats.maxDrawdown)}
                        
                        Act as a professional risk manager. Give me a 3-sentence aggressive terminal-style summary of my trading performance. 
                        Be raw, data-driven, and suggest one specific improvement.
                        """.trimIndent()

                    var insights = ""
                    aiProvider.sendMessage(prompt).collect { chunk ->
                        insights += chunk
                    }

                    _uiState.value = PerformanceUiState.Success(stats, insights)
                } catch (e: Exception) {
                    _uiState.value = PerformanceUiState.Success(stats, "AI_LINK_OFFLINE: UNABLE TO GENERATE INSIGHTS.")
                }
            }
        }
    }
