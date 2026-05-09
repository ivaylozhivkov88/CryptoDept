package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.model.FundingHeatmapItem
import com.cryptodept.domain.model.*
import com.cryptodept.domain.repository.DerivativesRepository
import com.cryptodept.domain.usecase.LiquidationPredictionEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DerivativesViewModel
    @Inject
    constructor(
        private val derivativesRepository: DerivativesRepository,
        private val predictionEngine: LiquidationPredictionEngine,
    ) : ViewModel() {
        private val _selectedCoin = MutableStateFlow("BTC")
        private val _state = MutableStateFlow<DerivativesUiState>(DerivativesUiState.Loading)
        val state: StateFlow<DerivativesUiState> = _state.asStateFlow()

        init {
            load()
        }

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
                    val heatmap = derivativesRepository.getFundingHeatmap()

                    if (funding.isSuccess) {
                        val fundingData = funding.getOrThrow()
                        val liqData = liq.getOrNull()
                        val magneticZones = if (liqData != null) {
                            predictionEngine.predictMagneticZones(fundingData.markPrice, liqData)
                        } else emptyList()

                        _state.value =
                            DerivativesUiState.Success(
                                fundingData,
                                oi.getOrNull(),
                                liqData,
                                heatmap.getOrDefault(emptyList()),
                                magneticZones,
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
        val liquidations: LiquidationData?,
        val heatmap: List<FundingHeatmapItem>,
        val magneticZones: List<MagneticZone>,
    ) : DerivativesUiState()

    data class Error(
        val message: String,
    ) : DerivativesUiState()
}
