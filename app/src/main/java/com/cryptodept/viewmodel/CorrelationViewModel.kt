package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.model.CorrelationMatrix
import com.cryptodept.domain.usecase.GetCorrelationMatrixUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<CorrelationUiState>(CorrelationUiState.Loading)
        val uiState: StateFlow<CorrelationUiState> = _uiState.asStateFlow()

        private val defaultSymbols =
            listOf(
                "BTC",
                "ETH",
                "SOL",
                "XRP",
                "ADA",
                "DOT",
                "LINK",
                "AVAX",
                "MATIC",
                "DOGE",
            )

        init {
            loadCorrelationMatrix()
        }

        fun loadCorrelationMatrix(
            symbols: List<String> = defaultSymbols,
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
