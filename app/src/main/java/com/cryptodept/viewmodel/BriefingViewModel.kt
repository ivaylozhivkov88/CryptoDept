package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.repository.CryptoRepository
import com.cryptodept.domain.repository.DerivativesRepository
import com.cryptodept.domain.usecase.DailyBriefingGenerator
import com.cryptodept.domain.usecase.RiskScoreEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BriefingViewModel
    @Inject
    constructor(
        private val cryptoRepository: CryptoRepository,
        private val derivativesRepository: DerivativesRepository,
        private val briefingGenerator: DailyBriefingGenerator,
        private val riskEngine: RiskScoreEngine,
    ) : ViewModel() {
        private val _briefingState = MutableStateFlow<BriefingUiState>(BriefingUiState.Loading)
        val briefingState: StateFlow<BriefingUiState> = _briefingState.asStateFlow()

        init {
            generateBriefing()
        }

        fun generateBriefing() {
            viewModelScope.launch {
                _briefingState.value = BriefingUiState.Loading
                try {
                    val btcPrice = cryptoRepository.getCachedPrice("bitcoin")
                    val btcChange = cryptoRepository.getCachedChange24h("bitcoin")
                    val fundingResult = derivativesRepository.getFundingRate("BTC")
                    val funding =
                        fundingResult.getOrNull()
                            ?: throw Exception("FUNDING DATA UNAVAILABLE")

                    val riskScore =
                        riskEngine.calculate(
                            rsi = 50.0,
                            fundingRate = funding.binanceRate,
                            longShortRatio = 1.5,
                            fearGreedIndex = 50,
                            exchangeInflowChange = 0.0,
                            openInterestChange = 0.0,
                            priceChange24h = btcChange,
                            // whaleSellingDetected е премахнат тук
                        )

                    val briefing =
                        briefingGenerator.generate(
                            btcPrice = btcPrice,
                            btcChange24h = btcChange,
                            riskScore = riskScore,
                            fundingRate = funding.binanceRate,
                            fearGreedIndex = 50,
                            exchangeInflowChange = 0.0,
                            upcomingEvents = emptyList(),
                            topLiquidationLevel = null,
                            // topWhaleAlerts е премахнат тук
                        )
                    _briefingState.value = BriefingUiState.Success(briefing)
                } catch (e: Exception) {
                    _briefingState.value = BriefingUiState.Error(e.message ?: "BRIEFING FAILED")
                }
            }
        }
    }

sealed class BriefingUiState {
    object Loading : BriefingUiState()

    data class Success(
        val briefing: DailyBriefingGenerator.DailyBriefing,
    ) : BriefingUiState()

    data class Error(
        val message: String,
    ) : BriefingUiState()
}
