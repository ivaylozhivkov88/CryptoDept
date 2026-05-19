package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.model.CorrelationMatrix
import com.cryptodept.domain.usecase.GetCorrelationMatrixUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CorrelationUiState {
    object Loading : CorrelationUiState()

    data class Success(
        val matrix: CorrelationMatrix,
    ) : CorrelationUiState()

    data class Error(
        val message: String,
    ) : CorrelationUiState()
}

@HiltViewModel
class CorrelationViewModel
    @Inject
    constructor(
        private val getCorrelationMatrixUseCase: GetCorrelationMatrixUseCase,
        private val repository: com.cryptodept.domain.repository.CryptoRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<CorrelationUiState>(CorrelationUiState.Loading)
        val uiState: StateFlow<CorrelationUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                repository.getTrackedCoinPrices().collectLatest { prices ->
                    val symbols = if (prices.isNotEmpty()) {
                        prices.map { it.symbol.uppercase() }
                    } else {
                        listOf("BTC", "ETH", "SOL", "XRP", "ADA", "DOT", "LINK", "AVAX", "MATIC", "DOGE")
                    }
                    loadCorrelationMatrix(symbols)
                }
            }
        }

        fun loadCorrelationMatrix(
            symbols: List<String>,
            days: Int = 30,
        ) {
            viewModelScope.launch {
                _uiState.value = CorrelationUiState.Loading
                getCorrelationMatrixUseCase
                    .execute(symbols, days)
                    .onSuccess { matrix ->
                        _uiState.value = CorrelationUiState.Success(matrix)
                    }.onFailure { error ->
                        _uiState.value = CorrelationUiState.Error(error.message ?: "Calculation failed")
                    }
            }
        }
    }
