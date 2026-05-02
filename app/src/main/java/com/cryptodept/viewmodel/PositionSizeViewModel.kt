package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.data.datastore.PreferencesManager
import com.cryptodept.domain.model.*
import com.cryptodept.domain.repository.CryptoRepository
import com.cryptodept.domain.repository.JournalRepository
import com.cryptodept.domain.usecase.PositionSizeCalculator
import com.cryptodept.domain.usecase.RiskScoreEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PositionSizeViewModel @Inject constructor(
    private val calculator: PositionSizeCalculator,
    private val cryptoRepository: CryptoRepository,
    private val riskEngine: RiskScoreEngine,
    private val journalRepository: JournalRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    // Inputs
    private val _portfolioSize = MutableStateFlow(10000.0)
    private val _riskPercent = MutableStateFlow(2.0)
    private val _entryPrice = MutableStateFlow(0.0)
    private val _stopLoss = MutableStateFlow(0.0)
    private val _takeProfit = MutableStateFlow(0.0)
    private val _selectedCoin = MutableStateFlow("bitcoin")

    val portfolioSize: StateFlow<Double> = _portfolioSize.asStateFlow()
    val riskPercent: StateFlow<Double> = _riskPercent.asStateFlow()
    val entryPrice: StateFlow<Double> = _entryPrice.asStateFlow()
    val stopLoss: StateFlow<Double> = _stopLoss.asStateFlow()
    val takeProfit: StateFlow<Double> = _takeProfit.asStateFlow()

    // Output
    private val _result = MutableStateFlow<PositionSizeResult?>(null)
    val result: StateFlow<PositionSizeResult?> = _result.asStateFlow()

    private val _currentRiskScore = MutableStateFlow(50)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val price = cryptoRepository.getCachedPrice("bitcoin")
            if (price > 0) {
                withContext(Dispatchers.Main) {
                    _entryPrice.value = price
                    _stopLoss.value = price * 0.97
                    _takeProfit.value = price * 1.06
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

    fun setPortfolioSize(value: Double) { _portfolioSize.value = value; calculate() }
    fun setRiskPercent(value: Double) { _riskPercent.value = value.coerceIn(0.1, 10.0); calculate() }
    fun setEntryPrice(value: Double) { _entryPrice.value = value; calculate() }
    fun setStopLoss(value: Double) { _stopLoss.value = value; calculate() }
    fun setTakeProfit(value: Double) { _takeProfit.value = value; calculate() }

    fun setTradeParams(entry: Double, sl: Double, tp: Double) {
        _entryPrice.value = entry
        _stopLoss.value = sl
        _takeProfit.value = tp
        calculate()
    }

    fun useCurrentPrice() {
        viewModelScope.launch(Dispatchers.IO) {
            val price = cryptoRepository.getCachedPrice(_selectedCoin.value)
            if (price > 0) { 
                withContext(Dispatchers.Main) {
                    _entryPrice.value = price
                    _stopLoss.value = price * 0.97
                    _takeProfit.value = price * 1.06
                    calculate() 
                }
            }
        }
    }

    fun saveToJournal() {
        val currentResult = _result.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val trade = TradeJournal(
                id = java.util.UUID.randomUUID().toString(),
                coinId = _selectedCoin.value,
                symbol = _selectedCoin.value.uppercase(),
                direction = if (currentResult.takeProfitPrice > currentResult.entryPrice) TradeDirection.LONG else TradeDirection.SHORT,
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
                marketConditions = "{}"
            )
            journalRepository.addTrade(trade)
        }
    }

    private fun calculate() {
        val entry = _entryPrice.value
        val sl = _stopLoss.value
        val tp = _takeProfit.value
        if (entry <= 0 || sl <= 0 || tp <= 0) return

        viewModelScope.launch(Dispatchers.Default) {
            val res = calculator.calculate(
                portfolioSize = _portfolioSize.value,
                riskPercent = _riskPercent.value,
                entryPrice = entry,
                stopLossPrice = sl,
                takeProfitPrice = tp,
                currentRiskScore = _currentRiskScore.value
            )
            withContext(Dispatchers.Main) {
                _result.value = res
            }
        }
    }
}
