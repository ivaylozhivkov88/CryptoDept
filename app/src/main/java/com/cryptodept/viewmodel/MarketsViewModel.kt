package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.model.CoinPrice
import com.cryptodept.domain.repository.CryptoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MarketsUiState {
    object Loading : MarketsUiState()

    data class Success(
        val coins: List<CoinPrice>,
    ) : MarketsUiState()

    data class Error(
        val message: String,
    ) : MarketsUiState()
}

@HiltViewModel
class MarketsViewModel
    @Inject
    constructor(
        private val cryptoRepository: CryptoRepository,
        private val sentimentAnalyzer: com.cryptodept.domain.usecase.SentimentAnalyzer,
        private val errorMapper: com.cryptodept.util.ErrorMessageMapper,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<MarketsUiState>(MarketsUiState.Loading)
        val uiState: StateFlow<MarketsUiState> = _uiState.asStateFlow()

        private val _sentimentMap = MutableStateFlow<Map<String, com.cryptodept.domain.usecase.SentimentVerdict>>(emptyMap())
        val sentimentMap: StateFlow<Map<String, com.cryptodept.domain.usecase.SentimentVerdict>> = _sentimentMap.asStateFlow()

        private val _errorChannel = Channel<String>()
        val errorEvents = _errorChannel.receiveAsFlow()

        init {
            observePrices()
            refreshData()
        }

        private fun observePrices() {
            viewModelScope.launch(Dispatchers.IO) {
                cryptoRepository
                    .getAllCoinPrices()
                    .catch { e ->
                        _uiState.value = MarketsUiState.Error(e.message ?: "DATABASE ERROR")
                    }.collect { coins ->
                        if (coins.isNotEmpty()) {
                            _uiState.value = MarketsUiState.Success(coins)
                            fetchQuickSentiment(coins.take(10))
                        } else if (_uiState.value is MarketsUiState.Loading) {
                            // If DB is empty and we are still loading, wait a bit then check again or error
                            delay(10000)
                            if (_uiState.value is MarketsUiState.Loading) {
                                _uiState.value = MarketsUiState.Error("NO DATA AVAILABLE. CHECK CONNECTION.")
                            }
                        }
                    }
            }
        }

        fun toggleTracking(coinId: String) {
            viewModelScope.launch(Dispatchers.IO) {
                cryptoRepository
                    .toggleTracking(coinId)
                    .onFailure { error ->
                        _errorChannel.trySend(errorMapper.map(error))
                    }
            }
        }

        private fun fetchQuickSentiment(coins: List<CoinPrice>) {
            viewModelScope.launch(Dispatchers.IO) {
                val map = mutableMapOf<String, com.cryptodept.domain.usecase.SentimentVerdict>()
                coins.forEach { coin ->
                    try {
                        val result = sentimentAnalyzer.analyzeCoin(coin.symbol.uppercase())
                        map[coin.id] = result.verdict
                    } catch (e: Exception) {
                    }
                }
                _sentimentMap.value = map
            }
        }

        private fun refreshData() {
            viewModelScope.launch(Dispatchers.IO) {
                cryptoRepository
                    .refreshPrices()
                    .onFailure { error ->
                        if (_uiState.value is MarketsUiState.Loading) {
                            launch {
                                val cached = cryptoRepository.getTrackedCoinPrices().first()
                                if (cached.isNotEmpty()) {
                                    _uiState.value = MarketsUiState.Success(cached)
                                } else {
                                    _uiState.value =
                                        MarketsUiState.Error(
                                            errorMapper.map(error),
                                        )
                                }
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
