package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.model.CoinPrice
import com.cryptodept.domain.repository.CryptoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class MarketsUiState {
    object Loading : MarketsUiState()
    data class Success(val coins: List<CoinPrice>) : MarketsUiState()
    data class Error(val message: String) : MarketsUiState()
}

@HiltViewModel
class MarketsViewModel @Inject constructor(
    private val cryptoRepository: CryptoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MarketsUiState>(MarketsUiState.Loading)
    val uiState: StateFlow<MarketsUiState> = _uiState.asStateFlow()

    init {
        observePrices()
        refreshData()
    }

    private fun observePrices() {
        viewModelScope.launch(Dispatchers.IO) {
            cryptoRepository.getTrackedCoinPrices()
                .catch { e ->
                    _uiState.value = MarketsUiState.Error(e.message ?: "DATABASE ERROR")
                }
                .collect { coins ->
                    if (coins.isNotEmpty()) {
                        _uiState.value = MarketsUiState.Success(coins)
                    }
                }
        }
    }

    private fun refreshData() {
        viewModelScope.launch(Dispatchers.IO) {
            cryptoRepository.refreshPrices()
                .onFailure { error ->
                    if (_uiState.value is MarketsUiState.Loading) {
                        val cached = cryptoRepository.getTrackedCoinPrices().first()
                        if (cached.isNotEmpty()) {
                            _uiState.value = MarketsUiState.Success(cached)
                        } else {
                            _uiState.value = MarketsUiState.Error(
                                error.message ?: "FAILED TO LOAD MARKET DATA"
                            )
                        }
                    }
                }
        }
    }

    fun loadMarkets() {
        _uiState.value = MarketsUiState.Loading
        refreshData()
    }
}