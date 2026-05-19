package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.repository.CryptoRepository
import com.cryptodept.domain.usecase.BacktesterEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class BacktesterViewModel
    @Inject
    constructor(
        private val repository: CryptoRepository,
        private val backtesterEngine: BacktesterEngine,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<BacktestUiState>(BacktestUiState.Idle)
        val uiState: StateFlow<BacktestUiState> = _uiState.asStateFlow()

        val trackedCoins: StateFlow<List<String>> =
            repository
                .getTrackedCoinPrices()
                .map { prices -> prices.map { it.id } }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Form State
        var selectedCoin = MutableStateFlow("bitcoin")
        var rsiEntry = MutableStateFlow(35f)
        var rsiExit = MutableStateFlow(65f)
        var stopLoss = MutableStateFlow(5f)
        var takeProfit = MutableStateFlow(10f)
        var initialCapital = MutableStateFlow(10000f)
        var riskPerTrade = MutableStateFlow(2f)

        fun runBacktest() {
            viewModelScope.launch {
                _uiState.value = BacktestUiState.Loading
                try {
                    val config =
                        BacktesterEngine.BacktestConfig(
                            coinId = selectedCoin.value,
                            startDate = 0,
                            endDate = System.currentTimeMillis(),
                            rsiEntryThreshold = rsiEntry.value.toDouble(),
                            rsiExitThreshold = rsiExit.value.toDouble(),
                            stopLossPercent = stopLoss.value.toDouble(),
                            takeProfitPercent = takeProfit.value.toDouble(),
                            initialCapital = initialCapital.value.toDouble(),
                            riskPerTradePercent = riskPerTrade.value.toDouble(),
                        )

                    // Increase historical data range (Task 2.5)
                    val history =
                        withContext(Dispatchers.IO) {
                            repository.getOHLCData(config.coinId, days = 90)
                        }

                    if (history.isEmpty()) {
                        _uiState.value = BacktestUiState.Error("NO HISTORICAL DATA FOR THIS PERIOD")
                        return@launch
                    }

                    val result =
                        withContext(Dispatchers.Default) {
                            backtesterEngine.run(config, history)
                        }
                    _uiState.value = BacktestUiState.Success(result)
                } catch (e: Exception) {
                    _uiState.value = BacktestUiState.Error("[ERROR] ${e.message ?: "BACKTEST FAILED"}")
                }
            }
        }
    }

sealed class BacktestUiState {
    object Idle : BacktestUiState()

    object Loading : BacktestUiState()

    data class Success(
        val result: BacktesterEngine.BacktestResult,
    ) : BacktestUiState()

    data class Error(
        val message: String,
    ) : BacktestUiState()
}
