package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.data.datastore.PreferencesService
import com.cryptodept.domain.model.*
import com.cryptodept.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

sealed class AnalysisUiState {
    object Loading : AnalysisUiState()
    data class Success(val result: DeepAnalysisResult) : AnalysisUiState()
    data class Error(val message: String) : AnalysisUiState()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val runDeepAnalysis: RunDeepAnalysisUseCase,
    private val generateReport: GenerateAnalysisReportUseCase,
    private val observeAnalysisHistory: ObserveAnalysisHistoryUseCase,
    private val preferencesService: PreferencesService,
) : ViewModel() {

    val isAdmin = preferencesService.isAdmin.stateIn(
        viewModelScope, 
        SharingStarted.WhileSubscribed(5000), 
        false
    )

    private val _selectedCoin = MutableStateFlow("bitcoin")
    private val _selectedDays = MutableStateFlow(30)

    val analysisState: StateFlow<AnalysisUiState> = combine(
        _selectedCoin, 
        _selectedDays
    ) { coin, days -> coin to days }
        .flatMapLatest { (coin, days) ->
            flow {
                emit(AnalysisUiState.Loading)
                try {
                    withTimeout(20000) {
                        runDeepAnalysis.execute(coin, days)
                            .onSuccess { emit(AnalysisUiState.Success(it)) }
                            .onFailure { emit(AnalysisUiState.Error(it.message ?: "UNKNOWN ERROR")) }
                    }
                } catch (e: TimeoutCancellationException) {
                    emit(AnalysisUiState.Error("ANALYSIS_TIMEOUT: NETWORK_CONGESTION_DETECTED"))
                } catch (e: Exception) {
                    emit(AnalysisUiState.Error(e.message ?: "SYSTEM_ERROR"))
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalysisUiState.Loading)

    fun loadAnalysis(coinId: String) {
        // Handle both "bitcoin" (ID) and "BTC" (Symbol)
        val cleanId = coinId.lowercase()
        _selectedCoin.value = if (cleanId == "btc") "bitcoin" else if (cleanId == "eth") "ethereum" else cleanId
    }

    val trackedCoins: StateFlow<List<String>> = observeAnalysisHistory()
        .map { list -> 
            if (list.isEmpty()) listOf("BTC", "ETH", "SOL", "BNB", "XRP", "DOGE", "ADA", "TRX", "DOT", "LINK", "AVAX", "SHIB", "TON", "XLM", "SUI")
            else list 
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("BTC", "ETH", "SOL"))

    private val _aiReport = MutableStateFlow<String?>(null)
    val aiReport: StateFlow<String?> = _aiReport.asStateFlow()

    private val _isAiStreaming = MutableStateFlow(false)
    val isAiStreaming: StateFlow<Boolean> = _isAiStreaming.asStateFlow()

    fun generateAIReport(result: DeepAnalysisResult) {
        viewModelScope.launch {
            _aiReport.value = ""
            _isAiStreaming.value = true
            generateReport.execute(result)
                .onCompletion { _isAiStreaming.value = false }
                .collect { chunk ->
                    _aiReport.value = (_aiReport.value ?: "") + chunk
                }
        }
    }

    fun dismissAiReport() {
        _aiReport.value = null
    }

    fun setAdminStatus(isAdmin: Boolean) {
        viewModelScope.launch { preferencesService.setAdminStatus(isAdmin) }
    }
}
