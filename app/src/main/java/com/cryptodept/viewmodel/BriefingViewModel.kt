package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.repository.CryptoRepository
import com.cryptodept.domain.repository.DerivativesRepository
import com.cryptodept.domain.usecase.DailyBriefingGenerator
import com.cryptodept.domain.usecase.RiskScoreEngine
import com.cryptodept.domain.usecase.GetNetworkHealthUseCase
import com.cryptodept.domain.usecase.GetMacroIntelligenceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
        private val getNetworkHealth: GetNetworkHealthUseCase,
        private val getMacroIntelligence: GetMacroIntelligenceUseCase,
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
                    coroutineScope {
                        val btcPriceDeferred = async { cryptoRepository.getCurrentPrice("bitcoin") }
                        val btcChangeDeferred = async { cryptoRepository.getCachedChange24h("bitcoin") }
                        val fundingDeferred = async { derivativesRepository.getFundingRate("BTC") }
                        val healthDeferred = async { getNetworkHealth() }
                        val macroDeferred = async { getMacroIntelligence() }
                        val lsRatioDeferred = async { derivativesRepository.getLongShortRatio("BTC") }

                        val btcPrice = btcPriceDeferred.await()
                        val btcChange = btcChangeDeferred.await()
                        val funding = fundingDeferred.await().getOrNull() ?: throw Exception("DERIVATIVES_OFFLINE")
                        val health = healthDeferred.await().getOrNull()
                        val macro = macroDeferred.await().getOrNull()
                        val lsRatio = lsRatioDeferred.await().getOrNull()?.let { it.first / (it.first + it.second) } ?: 1.0

                        val riskScore = riskEngine.calculate(
                            rsi = 50.0, // Default to neutral if no history
                            fundingRate = funding.binanceRate,
                            longShortRatio = lsRatio,
                            fearGreedIndex = health?.fearGreedIndex ?: 50,
                            exchangeInflowChange = 0.0,
                            openInterestChange = 0.0,
                            priceChange24h = btcChange,
                        )

                        val briefing = briefingGenerator.generate(
                            btcPrice = btcPrice,
                            btcChange24h = btcChange,
                            riskScore = riskScore,
                            fundingRate = funding.binanceRate,
                            fearGreedIndex = health?.fearGreedIndex ?: 50,
                            exchangeInflowChange = 0.0,
                            upcomingEvents = emptyList(),
                            topLiquidationLevel = null,
                        )
                        _briefingState.value = BriefingUiState.Success(briefing)
                    }
                } catch (e: Exception) {
                    _briefingState.value = BriefingUiState.Error(e.message ?: "BRIEFING_FAILED")
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
