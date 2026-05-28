package com.cryptodept.ui.prediction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.cryptodept.data.db.PredictionAccuracyEntity
import com.cryptodept.domain.model.PricePrediction
import com.cryptodept.domain.repository.CryptoRepository
import com.cryptodept.domain.prediction.ConfidenceMetrics
import com.cryptodept.domain.prediction.HistoricalAccuracy
import com.cryptodept.domain.usecase.prediction.CalculateConfidenceMetricsUseCase
import com.cryptodept.domain.usecase.prediction.PredictionEnsembleEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PredictionViewModel
    @Inject
    constructor(
        private val ensembleEngine: PredictionEnsembleEngine,
        private val repository: CryptoRepository,
        private val remoteDataSource: com.cryptodept.data.remote.source.FirebaseRemoteDataSource,
        private val accuracyTracker: com.cryptodept.domain.usecase.PredictionAccuracyTracker,
        private val calculateConfidenceUseCase: CalculateConfidenceMetricsUseCase,
        private val generateReport: com.cryptodept.domain.usecase.GenerateAnalysisReportUseCase,
    ) : ViewModel() {

        private val _aiReport = MutableStateFlow<String?>(null)
        val aiReport: StateFlow<String?> = _aiReport.asStateFlow()

        private val _isAiStreaming = MutableStateFlow(false)
        val isAiStreaming: StateFlow<Boolean> = _isAiStreaming.asStateFlow()

        private val _historicalData = MutableStateFlow<List<com.cryptodept.domain.model.OHLCData>>(emptyList())
        val historicalData: StateFlow<List<com.cryptodept.domain.model.OHLCData>> = _historicalData.asStateFlow()

        fun generateAIReport(prediction: PricePrediction) {
            viewModelScope.launch {
                try {
                    _aiReport.value = ""
                    _isAiStreaming.value = true
                    generateReport.execute(prediction, _confidenceMetrics.value)
                        .onCompletion { _isAiStreaming.value = false }
                        .catch { e -> 
                            val message = e.localizedMessage ?: "UNKNOWN_AI_FAILURE"
                            val errorNote = if (message.contains("blocked", ignoreCase = true)) {
                                "\n\nNOTE: API Key is restricted. Ensure your Package Name (com.cryptodept) and SHA-1 fingerprint are registered in Google AI Studio."
                            } else ""
                            _aiReport.value = ">>> ERROR_STREAM_INTERRUPTED: $message$errorNote"
                            _isAiStreaming.value = false
                        }
                        .collect { chunk ->
                            _aiReport.value = (_aiReport.value ?: "") + chunk
                        }
                } catch (e: Exception) {
                    _aiReport.value = ">>> CRITICAL_SYSTEM_ERROR: ${e.localizedMessage}"
                    _isAiStreaming.value = false
                }
            }
        }

        fun dismissAiReport() {
            _aiReport.value = null
        }
        val history: Flow<PagingData<PredictionAccuracyEntity>> =
            accuracyTracker
                .getHistoryPagingData()
                .cachedIn(viewModelScope)

        private val _uiState = MutableStateFlow<PredictUiState>(PredictUiState.Idle)
        val uiState: StateFlow<PredictUiState> = _uiState

        private val _accuracyStats = MutableStateFlow<Map<String, Float>>(emptyMap())
        val accuracyStats: StateFlow<Map<String, Float>> = _accuracyStats

        private val _confidenceMetrics = MutableStateFlow<ConfidenceMetrics?>(null)
        val confidenceMetrics: StateFlow<ConfidenceMetrics?> = _confidenceMetrics

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
                    // --- PHASE O: CLOUD-FIRST STRATEGY ---
                    currentLogs.add("INTERROGATING_CRYPTODEPT_CLOUD")
                    _uiState.value = PredictUiState.Loading(currentLogs.toList(), 0.1f)
                    
                    val cloudData = remoteDataSource.getCloudPrediction(coinId)
                    if (cloudData != null) {
                        currentLogs.add("CLOUD_DATA_RETRIEVED_SUCCESSFULLY")
                        currentLogs.add("DECRYPTING_QUANT_PACKAGE")
                        _uiState.value = PredictUiState.Loading(currentLogs.toList(), 0.8f)
                        delay(500)
                        
                        // Map simplified cloud data to domain model
                        val prediction = mapCloudDataToPrediction(coinId, cloudData)
                        _uiState.value = PredictUiState.Success(prediction)
                        return@launch
                    }

                    currentLogs.add("CLOUD_NODE_OFFLINE_OR_MISSING_DATA")
                    currentLogs.add("INITIATING_LOCAL_RESERVE_ENGINE")

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

                    // Start heavy computation in background immediately
                    val resultDeferred = async(Dispatchers.Default) {
                        ensembleEngine.generatePrediction(coinId, closes, volumes)
                    }

                    // Animate logs while computation is running
                    analysisSteps.forEachIndexed { index, step ->
                        currentLogs.add(step)
                        // Progress reaches 90% through logs, last 10% is for finishing the result
                        val progress = ((index + 1).toFloat() / analysisSteps.size) * 0.9f
                        _uiState.value = PredictUiState.Loading(currentLogs.toList(), progress)
                        
                        // Slightly faster typing for better UX
                        val typingDelay = (step.length * 8L) + 80L
                        delay(typingDelay)
                    }

                    // Ensure we wait for the result if computation takes longer than logs
                    val result = resultDeferred.await()
                    
                    _historicalData.value = history
                    _uiState.value = PredictUiState.Loading(currentLogs.toList(), 1.0f)
                    delay(300) // Small pause at 100% for visual completion
                    
                    _uiState.value = PredictUiState.Success(result)
                    loadConfidenceMetrics(coinId, result)
                } catch (e: Exception) {
                    _uiState.value = PredictUiState.Error("SYSTEM_CRASH: ${e.localizedMessage}")
                }
            }
        }

        private fun loadConfidenceMetrics(coinId: String, prediction: PricePrediction) {
            viewModelScope.launch {
                val metrics = calculateConfidenceUseCase.forEnsemble(
                    coinId = coinId,
                    consensus = prediction.ensembleConsensus,
                    currentPrice = prediction.currentPrice,
                    volatility24h = prediction.priceChange24h / 100.0, // Rough estimate if volatility is not directly available
                    lastDataUpdateMs = prediction.timestamp,
                    dataPointCount = 30 // Based on the 30 days history fetch
                )
                _confidenceMetrics.value = metrics
            }
        }

        suspend fun getModelAccuracy(coinId: String, modelName: String): HistoricalAccuracy? {
            return calculateConfidenceUseCase.forSingleModel(coinId, modelName)
        }

        fun reset() {
            _uiState.value = PredictUiState.Idle
        }
    }
