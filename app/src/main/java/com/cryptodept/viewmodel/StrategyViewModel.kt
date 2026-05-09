package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.model.OHLCData
import com.cryptodept.domain.model.StrategyRule
import com.cryptodept.domain.model.TradingStrategy
import com.cryptodept.domain.repository.CryptoRepository
import com.cryptodept.domain.usecase.BacktestEngine
import com.cryptodept.domain.usecase.BacktestReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class StrategyViewModel @Inject constructor(
    private val repository: CryptoRepository,
    private val backtestEngine: BacktestEngine
) : ViewModel() {

    private val _entryRules = MutableStateFlow<List<StrategyRule>>(emptyList())
    val entryRules: StateFlow<List<StrategyRule>> = _entryRules.asStateFlow()

    private val _exitRules = MutableStateFlow<List<StrategyRule>>(emptyList())
    val exitRules: StateFlow<List<StrategyRule>> = _exitRules.asStateFlow()

    private val _backtestReport = MutableStateFlow<BacktestReport?>(null)
    val backtestReport: StateFlow<BacktestReport?> = _backtestReport.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    fun addEntryRule(rule: StrategyRule) {
        _entryRules.value = _entryRules.value + rule
    }

    fun addExitRule(rule: StrategyRule) {
        _exitRules.value = _exitRules.value + rule
    }

    fun clearRules() {
        _entryRules.value = emptyList()
        _exitRules.value = emptyList()
        _backtestReport.value = null
    }

    fun runBacktest(coinId: String = "bitcoin") {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val history = withContext(Dispatchers.IO) {
                    repository.getOHLCData(coinId, 30)
                }

                val strategy = TradingStrategy(
                    id = "custom",
                    name = "My Strategy",
                    description = "Custom build",
                    entryRules = _entryRules.value,
                    exitRules = _exitRules.value
                )

                val report = withContext(Dispatchers.Default) {
                    backtestEngine.runBacktest(strategy, history)
                }
                
                _backtestReport.value = report
            } catch (e: Exception) {
                // handle error
            } finally {
                _isProcessing.value = false
            }
        }
    }
}
