package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.data.db.IntelligenceBriefingEntity
import com.cryptodept.domain.model.MarketDataSnapshot
import com.cryptodept.domain.repository.BriefingRepository
import com.cryptodept.domain.usecase.GetNetworkHealthUseCase
import com.cryptodept.domain.usecase.MultiAgentCoordinator
import com.cryptodept.domain.usecase.RiskScoreEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AgentHubViewModel @Inject constructor(
    private val briefingRepository: BriefingRepository,
    private val agentCoordinator: MultiAgentCoordinator,
    private val getNetworkHealth: GetNetworkHealthUseCase,
    private val riskEngine: RiskScoreEngine
) : ViewModel() {

    val briefings: StateFlow<List<IntelligenceBriefingEntity>> = briefingRepository.getAllBriefings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val healthResult = getNetworkHealth()
                if (healthResult.isSuccess) {
                    val health = healthResult.getOrThrow()
                    val snapshot = MarketDataSnapshot(
                        price = 0.0,
                        rsi = 50.0,
                        macdSignal = "N/A",
                        ema50Signal = "N/A",
                        ema200Signal = "N/A",
                        bollingerPosition = "N/A",
                        fundingRate = 0.0,
                        fundingLevel = "N/A",
                        longLiquidations24h = 0.0,
                        shortLiquidations24h = 0.0,
                        fearGreedIndex = health.fearGreedIndex,
                        newsSentiment = health.socialPulseLabel.uppercase(),
                        wyckoffPhase = "N/A",
                        elliottWave = "N/A",
                        riskScore = riskEngine.currentScore.value,
                        priceChange24h = 0.0,
                        btcDominance = 50.0,
                        sp500Change = 0.0,
                        dxyChange = 0.0,
                    )

                    val report = agentCoordinator.runOrchestration(snapshot)
                    
                    // Save the finding regardless of anomaly score if manual refresh
                    briefingRepository.saveBriefing(
                        IntelligenceBriefingEntity(
                            timestamp = System.currentTimeMillis(),
                            summary = report.summary,
                            anomalyScore = report.anomalyScore,
                            sentiment = snapshot.newsSentiment,
                            riskScore = snapshot.riskScore,
                            evidence = report.details.entries.joinToString { "${it.key}=${it.value}" }
                        )
                    )
                }
            } catch (_: Exception) {}
            _isRefreshing.value = false
        }
    }
}
