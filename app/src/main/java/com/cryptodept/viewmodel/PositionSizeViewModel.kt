package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.data.datastore.PreferencesManager
import com.cryptodept.domain.model.*
import com.cryptodept.domain.repository.CryptoRepository
import com.cryptodept.domain.repository.JournalRepository
import com.cryptodept.domain.usecase.CalculatePositionSizeUseCase
import com.cryptodept.domain.usecase.RiskScoreEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PositionSizeViewModel
    @Inject
    constructor(
        private val calculatePositionSize: CalculatePositionSizeUseCase,
        private val cryptoRepository: CryptoRepository,
        private val riskEngine: RiskScoreEngine,
        private val journalRepository: JournalRepository,
        private val preferencesManager: PreferencesManager,
    ) : ViewModel() {
        // Inputs as Strings for better UI handling
        private val _portfolioSize = MutableStateFlow("10000")
        private val _riskPercent = MutableStateFlow("2.0")
        private val _entryPrice = MutableStateFlow("")
        private val _stopLoss = MutableStateFlow("")
        private val _takeProfit = MutableStateFlow("")
        private val _selectedCoin = MutableStateFlow("bitcoin")

        val portfolioSize: StateFlow<String> = _portfolioSize.asStateFlow()
        val riskPercent: StateFlow<String> = _riskPercent.asStateFlow()
        val entryPrice: StateFlow<String> = _entryPrice.asStateFlow()
        val stopLoss: StateFlow<String> = _stopLoss.asStateFlow()
        val takeProfit: StateFlow<String> = _takeProfit.asStateFlow()

        // Output and State
        private val _uiState = MutableStateFlow<PositionSizeUiState>(PositionSizeUiState.Idle)
        val uiState: StateFlow<PositionSizeUiState> = _uiState.asStateFlow()

        private val _result = MutableStateFlow<PositionSizeResult?>(null)
        val result: StateFlow<PositionSizeResult?> = _result.asStateFlow()

        private val _currentRiskScore = MutableStateFlow(50)

        init {
            viewModelScope.launch(Dispatchers.IO) {
                val price = cryptoRepository.getCachedPrice("bitcoin")
                if (price > 0) {
                    withContext(Dispatchers.Main) {
                        _entryPrice.value = String.format(Locale.US, "%.4f", price)
                        _stopLoss.value = String.format(Locale.US, "%.4f", price * 0.97)
                        _takeProfit.value = String.format(Locale.US, "%.4f", price * 1.06)
                        calculate()
                    }
                }
            }

            viewModelScope.launch {
                riskEngine.observeRiskScore().collectLatest { score ->
                    _currentRiskScore.value = score
                    calculate()
                }
            }
        }

        fun setPortfolioSize(value: String) {
            _portfolioSize.value = value
            calculate()
        }

        fun setRiskPercent(value: String) {
            _riskPercent.value = value
            calculate()
        }

        fun setEntryPrice(value: String) {
            _entryPrice.value = value
            calculate()
        }

        fun setStopLoss(value: String) {
            _stopLoss.value = value
            calculate()
        }

        fun setTakeProfit(value: String) {
            _takeProfit.value = value
            calculate()
        }

        fun setTradeParams(
            entry: Double,
            sl: Double,
            tp: Double,
        ) {
            _entryPrice.value = String.format(Locale.US, "%.4f", entry)
            _stopLoss.value = String.format(Locale.US, "%.4f", sl)
            _takeProfit.value = String.format(Locale.US, "%.4f", tp)
            calculate()
        }

        fun useCurrentPrice() {
            viewModelScope.launch(Dispatchers.IO) {
                val price = cryptoRepository.getCachedPrice(_selectedCoin.value)
                if (price > 0) {
                    withContext(Dispatchers.Main) {
                        _entryPrice.value = String.format(Locale.US, "%.4f", price)
                        _stopLoss.value = String.format(Locale.US, "%.4f", price * 0.97)
                        _takeProfit.value = String.format(Locale.US, "%.4f", price * 1.06)
                        calculate()
                    }
                }
            }
        }

        fun saveToJournal() {
            val currentResult = _result.value ?: return
            viewModelScope.launch(Dispatchers.IO) {
                val trade =
                    TradeJournal(
                        id =
                            java.util.UUID
                                .randomUUID()
                                .toString(),
                        coinId = _selectedCoin.value,
                        symbol = _selectedCoin.value.uppercase(),
                        direction =
                            if (currentResult.takeProfitPrice >
                                currentResult.entryPrice
                            ) {
                                TradeDirection.LONG
                            } else {
                                TradeDirection.SHORT
                            },
                        entryPrice = currentResult.entryPrice,
                        exitPrice = null,
                        quantity = currentResult.positionSizeCoins,
                        entryTime = System.currentTimeMillis(),
                        exitTime = null,
                        riskPercent = currentResult.riskPercent,
                        stopLoss = currentResult.stopLossPrice,
                        takeProfit = currentResult.takeProfitPrice,
                        notes = "Calculated via Position Sizer. Risk Score: ${_currentRiskScore.value}",
                        status = TradeStatus.OPEN,
                        pnlUsd = null,
                        pnlPercent = null,
                        riskRewardActual = null,
                        positionSizeUsd = currentResult.positionSizeUsd,
                        marketConditions = "{}",
                    )
                journalRepository.addTrade(trade)
            }
        }

        private fun calculate() {
            val portfolio =
                _portfolioSize.value.toDoubleOrNull() ?: run {
                    _uiState.value = PositionSizeUiState.Error("INVALID PORTFOLIO SIZE")
                    return
                }
            val entry =
                _entryPrice.value.toDoubleOrNull() ?: run {
                    _uiState.value = PositionSizeUiState.Error("INVALID ENTRY PRICE")
                    return
                }
            val sl =
                _stopLoss.value.toDoubleOrNull() ?: run {
                    _uiState.value = PositionSizeUiState.Error("INVALID STOP LOSS")
                    return
                }
            val tp = _takeProfit.value.toDoubleOrNull() ?: 0.0

            val risk = _riskPercent.value.toDoubleOrNull()?.coerceIn(0.1, 100.0) ?: 1.0

            if (entry <= 0.0 || sl <= 0.0 || portfolio <= 0.0) {
                _uiState.value = PositionSizeUiState.Error("ALL VALUES MUST BE POSITIVE")
                return
            }
            if (entry == sl) {
                _uiState.value = PositionSizeUiState.Error("ENTRY PRICE CANNOT EQUAL STOP LOSS")
                return
            }

            _uiState.value = PositionSizeUiState.Idle

            viewModelScope.launch(Dispatchers.Default) {
                val res =
                    calculatePositionSize(
                        portfolioSize = portfolio,
                        riskPercent = risk,
                        entryPrice = entry,
                        stopLossPrice = sl,
                        takeProfitPrice = tp,
                        currentRiskScore = _currentRiskScore.value,
                    )
                withContext(Dispatchers.Main) {
                    _result.value = res
                }
            }
        }
    }

sealed class PositionSizeUiState {
    object Idle : PositionSizeUiState()

    data class Error(
        val message: String,
    ) : PositionSizeUiState()
}
