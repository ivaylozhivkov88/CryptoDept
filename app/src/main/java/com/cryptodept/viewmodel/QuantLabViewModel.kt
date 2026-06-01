package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.model.ModelVote
import com.cryptodept.domain.model.OHLCData
import com.cryptodept.domain.model.PriceDistribution
import com.cryptodept.domain.repository.CryptoRepository
import com.cryptodept.domain.usecase.prediction.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class QuantLabUiState {
    object Idle : QuantLabUiState()
    object Loading : QuantLabUiState()
    data class FourierSuccess(val coinId: String, val vote: ModelVote, val history: List<OHLCData>) : QuantLabUiState()
    data class MonteCarloSuccess(val coinId: String, val vote: ModelVote, val distribution: PriceDistribution, val history: List<OHLCData>) : QuantLabUiState()
    data class DynamismSuccess(val coinId: String, val hurst: Float, val fractalDim: Float, val history: List<OHLCData>) : QuantLabUiState()
    data class RegressionSuccess(val coinId: String, val vote: ModelVote, val history: List<OHLCData>) : QuantLabUiState()
    data class Error(val message: String) : QuantLabUiState()
}

@HiltViewModel
class QuantLabViewModel @Inject constructor(
    private val repository: CryptoRepository,
    private val fourierPredictor: FourierCyclePredictor,
    private val monteCarloPredictor: MonteCarloPredictor,
    private val hurstCalc: HurstExponentCalculator,
    private val fractalAnalyzer: FractalDimensionAnalyzer,
    private val linearRegression: LinearRegressionPredictor
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuantLabUiState>(QuantLabUiState.Idle)
    val uiState: StateFlow<QuantLabUiState> = _uiState.asStateFlow()

    fun runFourierScan(coinId: String) {
        viewModelScope.launch {
            _uiState.value = QuantLabUiState.Loading
            delay(500) // Ensure UI transition is smooth
            try {
                val history = fetchHistoryWithFallback(coinId, 32)
                if (history.isEmpty()) {
                    _uiState.value = QuantLabUiState.Error("DATA_NODE_TIMEOUT: Unable to retrieve $coinId history packets. Check connection.")
                    return@launch
                }
                val closes = history.map { it.close }
                val vote = withContext(Dispatchers.Default) { fourierPredictor.predict(closes, 24) }
                _uiState.value = QuantLabUiState.FourierSuccess(coinId, vote, history)
            } catch (e: Exception) {
                _uiState.value = QuantLabUiState.Error("FOURIER_ENGINE_CRASH: ${e.localizedMessage}")
            }
        }
    }

    fun runMonteCarloScan(coinId: String) {
        viewModelScope.launch {
            _uiState.value = QuantLabUiState.Loading
            delay(500)
            try {
                val history = fetchHistoryWithFallback(coinId, 30)
                if (history.isEmpty()) {
                    _uiState.value = QuantLabUiState.Error("DATA_NODE_TIMEOUT: Asset $coinId history unreachable.")
                    return@launch
                }
                val closes = history.map { it.close }
                val result = withContext(Dispatchers.Default) { monteCarloPredictor.simulate(closes, 24) }
                _uiState.value = QuantLabUiState.MonteCarloSuccess(coinId, result.first, result.second, history)
            } catch (e: Exception) {
                _uiState.value = QuantLabUiState.Error("PROBABILITY_ENGINE_CRASH: ${e.localizedMessage}")
            }
        }
    }

    fun runDynamismScan(coinId: String) {
        viewModelScope.launch {
            _uiState.value = QuantLabUiState.Loading
            delay(500)
            try {
                val history = fetchHistoryWithFallback(coinId, 50)
                if (history.isEmpty()) {
                    _uiState.value = QuantLabUiState.Error("DATA_NODE_TIMEOUT: Metadata sync failed for $coinId.")
                    return@launch
                }
                val closes = history.map { it.close }
                val hurst = withContext(Dispatchers.Default) { hurstCalc.calculate(closes) }
                val fractal = withContext(Dispatchers.Default) { fractalAnalyzer.calculate(closes) }
                _uiState.value = QuantLabUiState.DynamismSuccess(coinId, hurst, fractal, history)
            } catch (e: Exception) {
                _uiState.value = QuantLabUiState.Error("DYNAMISM_SCAN_FAILED: ${e.localizedMessage}")
            }
        }
    }

    fun runRegressionScan(coinId: String) {
        viewModelScope.launch {
            _uiState.value = QuantLabUiState.Loading
            delay(500)
            try {
                val history = fetchHistoryWithFallback(coinId, 30)
                if (history.isEmpty()) {
                    _uiState.value = QuantLabUiState.Error("DATA_NODE_TIMEOUT: Regression packets lost.")
                    return@launch
                }
                val closes = history.map { it.close }
                val vote = withContext(Dispatchers.Default) { linearRegression.predict(closes, 24) }
                _uiState.value = QuantLabUiState.RegressionSuccess(coinId, vote, history)
            } catch (e: Exception) {
                _uiState.value = QuantLabUiState.Error("REGRESSION_TUNNEL_CRASH: ${e.localizedMessage}")
            }
        }
    }

    private suspend fun fetchHistoryWithFallback(coinId: String, days: Int): List<OHLCData> {
        return withContext(Dispatchers.IO) {
            var data = repository.getOHLCData(coinId, days)
            if (data.isEmpty() && days > 7) {
                // Fallback to 7 days if 30+ fails
                data = repository.getOHLCData(coinId, 7)
            }
            data
        }
    }

    fun reset() {
        _uiState.value = QuantLabUiState.Idle
    }
}
