package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.repository.CryptoRepository
import com.cryptodept.domain.repository.DerivativesRepository
import com.cryptodept.data.api.FearGreedApi
import com.cryptodept.domain.usecase.RiskScoreEngine
import com.cryptodept.domain.usecase.TechnicalAnalysisEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RiskViewModel @Inject constructor(
    private val cryptoRepository: CryptoRepository,
    private val derivativesRepository: DerivativesRepository,
    private val fearGreedApi: FearGreedApi,
    private val riskEngine: RiskScoreEngine,
    private val taEngine: TechnicalAnalysisEngine
) : ViewModel() {

    private val _riskState = MutableStateFlow<RiskUiState>(RiskUiState.Loading)
    val riskState: StateFlow<RiskUiState> = _riskState.asStateFlow()

    init { calculateRisk() }

    fun calculateRisk() {
        viewModelScope.launch {
            _riskState.value = RiskUiState.Loading
            try {
                val btcPrice = cryptoRepository.getCachedPrice("bitcoin")
                val btcChange = cryptoRepository.getCachedChange24h("bitcoin")
                val ohlc = cryptoRepository.getOHLCData("bitcoin", 30)
                val prices = ohlc.map { it.close }

                val rsi = if (prices.size >= 14) taEngine.calculateRSI(prices) else 50.0
                val funding = derivativesRepository.getFundingRate("BTC")
                    .getOrNull()?.binanceRate ?: 0.0
                val fearGreedResponse = fearGreedApi.getFearGreedIndex()
                val fearGreed = fearGreedResponse.data.firstOrNull()
                    ?.value?.toIntOrNull() ?: 50

                val score = riskEngine.calculate(
                    rsi = rsi,
                    fundingRate = funding,
                    longShortRatio = 1.5,
                    fearGreedIndex = fearGreed,
                    exchangeInflowChange = 0.0,
                    openInterestChange = 0.0,
                    priceChange24h = btcChange
                    // whaleSellingDetected е премахнат[cite: 1]
                )
                _riskState.value = RiskUiState.Success(score, btcPrice)
            } catch (e: Exception) {
                _riskState.value = RiskUiState.Error(e.message ?: "RISK CALCULATION FAILED")
            }
        }
    }
}

sealed class RiskUiState {
    object Loading : RiskUiState()
    data class Success(val score: RiskScoreEngine.RiskScore, val btcPrice: Double) : RiskUiState()
    data class Error(val message: String) : RiskUiState()
}