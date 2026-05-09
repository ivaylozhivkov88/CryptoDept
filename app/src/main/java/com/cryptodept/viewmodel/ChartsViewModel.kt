// STEP 9: ChartsViewModel for managing OHLC data
// Created: 2024-05-22
// Dependencies: GetOHLCUseCase
// Used by: ChartsScreen

package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.model.OHLCData
import com.cryptodept.domain.usecase.GetOHLCUseCase
import com.cryptodept.domain.usecase.RefreshOHLCUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChartsViewModel
    @Inject
    constructor(
        private val getOHLCUseCase: GetOHLCUseCase,
        private val refreshOHLCUseCase: RefreshOHLCUseCase,
    ) : ViewModel() {
        private val _chartState = MutableStateFlow<ChartUiState>(ChartUiState.Loading)
        val chartState: StateFlow<ChartUiState> = _chartState.asStateFlow()

        fun loadChart(
            coinId: String,
            days: Int = 7,
        ) {
            viewModelScope.launch(Dispatchers.IO) {
                _chartState.value = ChartUiState.Loading

                // Start collecting the data flow
                launch {
                    getOHLCUseCase(coinId, days)
                        .catch { e -> _chartState.value = ChartUiState.Error(e.message ?: "DATABASE ERROR") }
                        .collect { data ->
                            if (data.isNotEmpty()) {
                                _chartState.value = ChartUiState.Success(data)
                            }
                        }
                }

                // Trigger refresh
                refreshOHLCUseCase(coinId, days).onFailure { e ->
                    if (_chartState.value is ChartUiState.Loading) {
                        _chartState.value = ChartUiState.Error(e.message ?: "FAILED TO FETCH MARKET DATA")
                    }
                }
            }
        }
    }

sealed class ChartUiState {
    object Loading : ChartUiState()

    data class Success(
        val data: List<OHLCData>,
    ) : ChartUiState()

    data class Error(
        val message: String,
    ) : ChartUiState()
}
