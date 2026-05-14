package com.cryptodept.ui.prediction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.cryptodept.data.db.PredictionAccuracyEntity
import com.cryptodept.domain.model.PricePrediction
import com.cryptodept.domain.repository.CryptoRepository
import com.cryptodept.domain.usecase.prediction.PredictionEnsembleEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class PredictionViewModel
    @Inject
    constructor(
        private val ensembleEngine: PredictionEnsembleEngine,
        private val repository: CryptoRepository,
        private val accuracyTracker: com.cryptodept.domain.usecase.PredictionAccuracyTracker,
    ) : ViewModel() {
        val history: Flow<PagingData<PredictionAccuracyEntity>> =
            accuracyTracker
                .getHistoryPagingData()
                .cachedIn(viewModelScope)

        private val _uiState = MutableStateFlow<PredictUiState>(PredictUiState.Idle)
        val uiState: StateFlow<PredictUiState> = _uiState

        private val _accuracyStats = MutableStateFlow<Map<String, Float>>(emptyMap())
        val accuracyStats: StateFlow<Map<String, Float>> = _accuracyStats

        init {
            loadAccuracyStats()
        }

        private fun loadAccuracyStats() {
            viewModelScope.launch {
                val models = listOf("FOURIER", "ELLIOTT", "FRACTAL", "ENSEMBLE", "MONTE_CARLO", "WYCKOFF_PHASE", "LINEAR_REGRESSION")
                val statsMap = mutableMapOf<String, Float>()
                models.forEach { model ->
                    accuracyTracker.getModelAccuracy(model).collect { acc ->
                        statsMap[model] = acc
                        _accuracyStats.value = statsMap.toMap()
                    }
                }
            }
        }

        private val analysisSteps =
            listOf(
                "ESTABLISHING_SECURE_CONNECTION",
                "FETCHING_HISTORICAL_DATA_PACKETS",
                "INTERROGATING_BINANCE_LIQUIDITY_POOLS", // PHASE X
                "SCANNING_OPEN_INTEREST_ANOMALIES", // PHASE X
                "CALCULATING_HURST_EXPONENT",
                "ANALYZING_FRACTAL_DIMENSION",
                "RUNNING_MONTE_CARLO_SIMULATIONS",
                "FOURIER_CYCLE_DETECTION",
                "WYCKOFF_PHASE_SCANNING",
                "ELLIOTT_WAVE_RECOGNITION",
                "COMPILING_FINAL_CONSENSUS",
            )

        fun startDeepAnalysis(coinId: String) {
            viewModelScope.launch {
                val currentLogs = mutableListOf<String>()
                _uiState.value = PredictUiState.Loading(currentLogs.toList(), 0f)

                try {
                    val history =
                        withContext(Dispatchers.IO) {
                            repository.getOHLCData(coinId, days = 30)
                        }
                    if (history.isEmpty()) {
                        _uiState.value = PredictUiState.Error("INSUFFICIENT_DATA_FOR_ANALYSIS")
                        return@launch
                    }

                    val closes = history.map { it.close }
                    val volumes = history.map { it.volume }

                    analysisSteps.forEachIndexed { index, step ->
                        currentLogs.add(step)
                        val progress = (index + 1).toFloat() / analysisSteps.size
                        _uiState.value = PredictUiState.Loading(currentLogs.toList(), progress)
                        val typingDelay = (step.length * 10L) + 100L
                        delay(typingDelay)
                    }

                    val result =
                        withContext(Dispatchers.Default) {
                            ensembleEngine.generatePrediction(coinId, closes, volumes)
                        }
                    _uiState.value = PredictUiState.Success(result)
                } catch (e: Exception) {
                    _uiState.value = PredictUiState.Error("SYSTEM_CRASH: ${e.localizedMessage}")
                }
            }
        }

        fun generateShareText(prediction: PricePrediction): String =
            buildString {
                val coinId = prediction.coinId
                val currentPrice = String.format(Locale.US, "%.2f", prediction.currentPrice)
                val consensus = prediction.ensembleConsensus
                val consensusPercent = (consensus.overallConfidence * 100).toInt()
                val dateFormat = SimpleDateFormat("HH:mm:ss dd.MM.yyyy", Locale.US)
                val formattedDate = dateFormat.format(prediction.timestamp)

                // TITLE
                append("════════════════════════════════════════\n")
                append("🚀 CRYPTODEPT DEEP QUANT ANALYSIS — $coinId\n")
                append("════════════════════════════════════════\n\n")

                // CURRENT STATE
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

                // LIQUIDITY DATA (PHASE X)
                prediction.liquidityInsight?.let { liq ->
                    append(">>> LIQUIDITY_&_ORDERFLOW\n")
                    append("OPEN_INTEREST: $${String.format(Locale.US, "%.1f", liq.openInterest / 1_000_000)}M (${String.format(Locale.US, "%.1f", liq.openInterestChange24h)}% change)\n")
                    append("FUNDING_RATE: ${String.format(Locale.US, "%.4f", liq.fundingRate)}%\n")
                    append("SENTIMENT_BIAS: ${liq.sentimentBias}\n\n")
                }

                // EVIDENCE CHAIN (PHASE X)
                if (prediction.evidenceChain.isNotEmpty()) {
                    append(">>> ORACLE_EVIDENCE_CHAIN\n")
                    prediction.evidenceChain.forEachIndexed { index, step ->
                        append("${index + 1}. ${step.title}: ${step.impact.name} (${(step.confidence * 100).toInt()}%)\n")
                    }
                    append("\n")
                }

                // THE VERDICT
                append(">>> THE_CRYPTODEPT_VERDICT\n")
                val verdict =
                    when {
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
                append(
                    "1H:  $${String.format(Locale.US, "%.2f", prediction.prediction1h.mid)} (${prediction.prediction1h.direction.name})\n",
                )
                append(
                    "4H:  $${String.format(Locale.US, "%.2f", prediction.prediction4h.mid)} (${prediction.prediction4h.direction.name})\n",
                )
                append(
                    "24H: $${String.format(
                        Locale.US,
                        "%.2f",
                        prediction.prediction24h.mid,
                    )} (${prediction.prediction24h.direction.name})\n",
                )
                append(
                    "7D:  $${String.format(
                        Locale.US,
                        "%.2f",
                        prediction.prediction7d.mid,
                    )} (${prediction.prediction7d.direction.name})\n\n",
                )

                // FOOTER
                append("════════════════════════════════════════\n")
                append("📊 Analysis: Ensemble of 7 Quantitative Models\n")
                append("⚠️  DISCLAIMER: Not financial advice. Trade at your own risk.\n")
                append("#CryptoDept #DeepQuantAnalysis #$coinId #Crypto\n")
                append("🚀 LIKE IF YOU'RE FOLLOWING THIS ANALYSIS!\n")
            }

        fun generateImagePrompt(prediction: PricePrediction): String {
            val coinId = prediction.coinId.uppercase()
            val price = String.format(Locale.US, "$%.2f", prediction.currentPrice)
            val change =
                if (prediction.priceChange24h >= 0) {
                    "+${String.format(Locale.US, "%.2f", prediction.priceChange24h)}%"
                } else {
                    "${String.format(Locale.US, "%.2f", prediction.priceChange24h)}%"
                }
            val trend = if (prediction.priceChange24h >= 0) "BULLISH RALLY" else "BEARISH REJECTION"
            val colorTheme = if (prediction.priceChange24h >= 0) "Electric Green and Cyan" else "Neon Red and Orange"

            return "Cinematic shot of a futuristic high-tech crypto trading command center. " +
                "In the center, a massive transparent holographic glass display showing a detailed glowing 3D candlestick chart for $coinId. " +
                "The screen displays a big bold text: '$coinId PRICE: $price' and a 'BREAKING NEWS: $trend ($change)' ticker tape at the bottom. " +
                "Background is a dark cyberpunk city skyline at night through a large window. " +
                "Aesthetic: $colorTheme glowing lights, hyper-realistic, 8k resolution, volumetric lighting, photorealistic textures, Bloomberg terminal style overlay. " +
                "The atmosphere is intense and professional trading environment --v 6.0"
        }

        fun reset() {
            _uiState.value = PredictUiState.Idle
        }
    }

sealed class PredictUiState {
    object Idle : PredictUiState()

    data class Loading(
        val logs: List<String>,
        val progress: Float,
    ) : PredictUiState()

    data class Success(
        val prediction: PricePrediction,
    ) : PredictUiState()

    data class Error(
        val message: String,
    ) : PredictUiState()
}
