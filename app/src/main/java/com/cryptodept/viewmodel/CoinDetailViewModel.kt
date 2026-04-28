package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.model.CoinDetail
import com.cryptodept.domain.model.OHLCData
import com.cryptodept.domain.repository.CryptoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CoinDetailUiState {
    object Loading : CoinDetailUiState()
    data class Success(val detail: CoinDetail, val ohlc: List<OHLCData>) : CoinDetailUiState()
    data class Error(val message: String) : CoinDetailUiState()
}

@HiltViewModel
class CoinDetailViewModel @Inject constructor(
    private val repository: CryptoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CoinDetailUiState>(CoinDetailUiState.Loading)
    val uiState: StateFlow<CoinDetailUiState> = _uiState.asStateFlow()

    fun loadCoinDetail(coinId: String) {
        viewModelScope.launch {
            _uiState.value = CoinDetailUiState.Loading
            
            val detailResult = repository.getCoinDetail(coinId)
            val ohlc = repository.getOHLCData(coinId, 30)

            detailResult.fold(
                onSuccess = { detail ->
                    _uiState.value = CoinDetailUiState.Success(detail, ohlc)
                },
                onFailure = { error ->
                    _uiState.value = CoinDetailUiState.Error(error.message ?: "Unknown error")
                }
            )
        }
    }
}
