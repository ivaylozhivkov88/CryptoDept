package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.model.FundingRateData
import com.cryptodept.domain.model.LiquidationData
import com.cryptodept.domain.model.OpenInterestData
import com.cryptodept.domain.repository.DerivativesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DerivativesViewModel @Inject constructor(
    private val derivativesRepository: DerivativesRepository
) : ViewModel() {

    private val _selectedCoin = MutableStateFlow("BTC")
    private val _state = MutableStateFlow<DerivativesUiState>(DerivativesUiState.Loading)
    val state: StateFlow<DerivativesUiState> = _state.asStateFlow()

    init { load() }

    fun selectCoin(symbol: String) {
        _selectedCoin.value = symbol
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _state.value = DerivativesUiState.Loading
            try {
                val coin = _selectedCoin.value
                val funding = derivativesRepository.getFundingRate(coin)
                val oi = derivativesRepository.getOpenInterest(coin)
                val liq = derivativesRepository.getLiquidationData(coin)

                if (funding.isSuccess) {
                    _state.value = DerivativesUiState.Success(
                        funding.getOrThrow(),
                        oi.getOrNull(),
                        liq.getOrNull()
                    )
                } else {
                    _state.value = DerivativesUiState.Error("DERIVATIVES DATA UNAVAILABLE")
                }
            } catch (e: Exception) {
                _state.value = DerivativesUiState.Error(e.message ?: "LOAD FAILED")
            }
        }
    }
}

sealed class DerivativesUiState {
    object Loading : DerivativesUiState()
    data class Success(
        val funding: FundingRateData,
        val openInterest: OpenInterestData?,
        val liquidations: LiquidationData?
    ) : DerivativesUiState()
    data class Error(val message: String) : DerivativesUiState()
}
