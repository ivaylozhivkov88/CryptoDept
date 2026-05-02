package com.cryptodept.ui.prediction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.model.PricePrediction
import com.cryptodept.domain.usecase.prediction.PredictionEnsembleEngine
import com.cryptodept.domain.repository.CryptoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PredictionViewModel @Inject constructor(
    private val ensembleEngine: PredictionEnsembleEngine,
    private val repository: CryptoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val uiState: StateFlow<AnalysisUiState> = _uiState

    private val analysisSteps = listOf(
        "ESTABLISHING_SECURE_CONNECTION",
        "FETCHING_HISTORICAL_DATA_PACKETS",
        "CALCULATING_HURST_EXPONENT",
        "ANALYZING_FRACTAL_DIMENSION",
        "RUNNING_MONTE_CARLO_SIMULATIONS",
        "FOURIER_CYCLE_DETECTION",
        "WYCKOFF_PHASE_SCANNING",
        "ELLIOTT_WAVE_RECOGNITION",
        "COMPILING_FINAL_CONSENSUS"
    )

    fun startDeepAnalysis(coinId: String) {
        viewModelScope.launch {
            val currentLogs = mutableListOf<String>()
            _uiState.value = AnalysisUiState.Loading(currentLogs.toList(), 0f)

            try {
                val history = withContext(Dispatchers.IO) {
                    repository.getOHLCData(coinId, days = 30)
                }
                if (history.isEmpty()) {
                    _uiState.value = AnalysisUiState.Error("INSUFFICIENT_DATA_FOR_ANALYSIS")
                    return@launch
                }

                val closes = history.map { it.close }
                val volumes = history.map { it.volume }

                analysisSteps.forEachIndexed { index, step ->
                    currentLogs.add(step)
                    val progress = (index + 1).toFloat() / analysisSteps.size
                    _uiState.value = AnalysisUiState.Loading(currentLogs.toList(), progress)
                    val typingDelay = (step.length * 10L) + 100L
                    delay(typingDelay)
                }

                val result = withContext(Dispatchers.Default) {
                    ensembleEngine.generatePrediction(coinId, closes, volumes)
                }
                _uiState.value = AnalysisUiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = AnalysisUiState.Error("SYSTEM_CRASH: ${e.localizedMessage}")
            }
        }
    }

    fun reset() { _uiState.value = AnalysisUiState.Idle }
}

sealed class AnalysisUiState {
    object Idle : AnalysisUiState()
    data class Loading(val logs: List<String>, val progress: Float) : AnalysisUiState()
    data class Success(val prediction: PricePrediction) : AnalysisUiState()
    data class Error(val message: String) : AnalysisUiState()
}