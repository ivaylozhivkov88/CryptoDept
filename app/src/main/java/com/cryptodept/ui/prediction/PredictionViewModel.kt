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
import java.text.SimpleDateFormat
import java.util.Locale

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

    fun generateShareText(prediction: PricePrediction): String {
        return buildString {
            val coinId = prediction.coinId
            val currentPrice = String.format(Locale.US, "%.2f", prediction.currentPrice)
            val consensus = prediction.ensembleConsensus
            val consensusPercent = (consensus.overallConfidence * 100).toInt()
            val dateFormat = SimpleDateFormat("HH:mm:ss dd.MM.yyyy", Locale.US)
            val formattedDate = dateFormat.format(prediction.timestamp)

            // ЗАГЛАВИЕ
            append("════════════════════════════════════════\n")
            append("🚀 CRYPTODEPT DEEP QUANT ANALYSIS — $coinId\n")
            append("════════════════════════════════════════\n\n")

            // ТЕКУЩО СЪСТОЯНИЕ
            append(">>> CURRENT_STATE\n")
            append("PRICE: $$$currentPrice\n")
            append("TIMESTAMP: $formattedDate\n")
            append("CONSENSUS: ${consensus.direction.name.replace("_", " ")} ($consensusPercent% confidence)\n\n")

            // ВСИЧКИ МОДЕЛИ С ТЕХНИТЕ АНАЛИЗИ
            consensus.modelVotes.forEach { (model, vote) ->
                append(">>> ${model.displayName.uppercase()}\n")
                append("DIRECTION: ${vote.direction.name.replace("_", " ")}\n")
                append("TARGET: $${String.format(Locale.US, "%.2f", vote.targetPrice)}\n")
                append("CONFIDENCE: ${(vote.confidence * 100).toInt()}%\n")
                append("WEIGHT: ${(vote.weight * 100).toInt()}%\n")
                append("ANALYSIS: ${vote.reasoning}\n\n")
            }

            // AGREEMENT SCORE
            append(">>> ENSEMBLE_AGREEMENT\n")
            append("MODELS_ALIGNED: ${(consensus.agreementScore * 100).toInt()}%\n")
            if (consensus.dissenterModels.isNotEmpty()) {
                append("DISSENTER_MODELS: ${consensus.dissenterModels.joinToString(", ") { it.displayName }}\n")
            }
            append("\n")

            // THE VERDICT
            append(">>> THE_CRYPTODEPT_VERDICT\n")
            val verdict = when {
                consensusPercent >= 70 -> "🟢 STRONG ${consensus.direction.name} — Ensemble conviction is HIGH"
                consensusPercent >= 55 -> "🟡 MILD ${consensus.direction.name} — Slight edge detected"
                consensusPercent in 45..54 -> "⚪ NEUTRAL — Market equilibrium"
                else -> "🔴 STRONG ${consensus.direction.name} — Risk is elevated"
            }
            append(verdict + "\n\n")

            // PROBABILITY DISTRIBUTION
            append(">>> PRICE_DISTRIBUTION\n")
            append("10TH_PERCENTILE: $${String.format(Locale.US, "%.2f", prediction.priceDistribution.percentile10)}\n")
            append("50TH_PERCENTILE: $${String.format(Locale.US, "%.2f", prediction.priceDistribution.percentile50)}\n")
            append("90TH_PERCENTILE: $${String.format(Locale.US, "%.2f", prediction.priceDistribution.percentile90)}\n")
            append("STD_DEVIATION: $${String.format(Locale.US, "%.2f", prediction.priceDistribution.standardDeviation)}\n\n")

            // TIMEFRAME TARGETS
            append(">>> MULTI_TIMEFRAME_TARGETS\n")
            append("1H:  $${String.format(Locale.US, "%.2f", prediction.prediction1h.mid)} (${prediction.prediction1h.direction.name})\n")
            append("4H:  $${String.format(Locale.US, "%.2f", prediction.prediction4h.mid)} (${prediction.prediction4h.direction.name})\n")
            append("24H: $${String.format(Locale.US, "%.2f", prediction.prediction24h.mid)} (${prediction.prediction24h.direction.name})\n")
            append("7D:  $${String.format(Locale.US, "%.2f", prediction.prediction7d.mid)} (${prediction.prediction7d.direction.name})\n\n")

            // FOOTER
            append("════════════════════════════════════════\n")
            append("📊 Analysis: Ensemble of 7 Quantitative Models\n")
            append("⚠️  DISCLAIMER: Not financial advice. Trade at your own risk.\n")
            append("#CryptoDept #DeepQuantAnalysis #$coinId #Crypto\n")
            append("🚀 LIKE IF YOU'RE FOLLOWING THIS ANALYSIS!\n")
        }
    }

    fun reset() {
        _uiState.value = AnalysisUiState.Idle
    }
}

sealed class AnalysisUiState {
    object Idle : AnalysisUiState()
    data class Loading(val logs: List<String>, val progress: Float) : AnalysisUiState()
    data class Success(val prediction: PricePrediction) : AnalysisUiState()
    data class Error(val message: String) : AnalysisUiState()
}