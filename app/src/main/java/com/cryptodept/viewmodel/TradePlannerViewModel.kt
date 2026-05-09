package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import com.cryptodept.domain.model.TradeDirectionType
import com.cryptodept.domain.model.TradeSetup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class TradePlannerViewModel
    @Inject
    constructor() : ViewModel() {
        private val _direction = MutableStateFlow(TradeDirectionType.LONG)
        val direction: StateFlow<TradeDirectionType> = _direction

        private val _entryPrice = MutableStateFlow(0.0)
        val entryPrice: StateFlow<Double> = _entryPrice

        private val _stopLoss = MutableStateFlow(0.0)
        val stopLoss: StateFlow<Double> = _stopLoss

        private val _takeProfit = MutableStateFlow(0.0)
        val takeProfit: StateFlow<Double> = _takeProfit

        private val _coinSymbol = MutableStateFlow("BTC")
        val coinSymbol: StateFlow<String> = _coinSymbol

        private val _setup = MutableStateFlow<TradeSetup?>(null)
        val setup: StateFlow<TradeSetup?> = _setup

        private val _isLoading = MutableStateFlow(false)
        val isLoading: StateFlow<Boolean> = _isLoading

        fun setInitialParams(
            symbol: String,
            entry: Double,
            sl: Double,
            tp: Double,
        ) {
            _coinSymbol.value = symbol
            _entryPrice.value = entry
            _stopLoss.value = sl
            _takeProfit.value = tp
        }

        fun setDirection(dir: TradeDirectionType) {
            _direction.value = dir
        }

        fun setEntryPrice(price: Double) {
            _entryPrice.value = price
        }

        fun setStopLoss(price: Double) {
            _stopLoss.value = price
        }

        fun setTakeProfit(price: Double) {
            _takeProfit.value = price
        }

        fun analyzeSetup() {
            // Тук добави твоята логика за проверка на риска
            // За момента само генерираме базов обект за UI-то
        }
    }
