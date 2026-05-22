package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.data.db.IntelligenceBriefingEntity
import com.cryptodept.data.remote.source.FirebaseRemoteDataSource
import com.cryptodept.domain.model.AgentStatus
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
    private val riskEngine: RiskScoreEngine,
    private val firebaseDataSource: FirebaseRemoteDataSource
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val briefings: StateFlow<List<IntelligenceBriefingEntity>> = combine(
        briefingRepository.getAllBriefings(),
        firebaseDataSource.getTerminalState()
    ) { local, cloud ->
        if (local.isNotEmpty()) {
            local.distinctBy { it.summary }
        } else {
            // Fallback: Map cloud specialized reports to briefing cards if local DB is empty
            cloud?.agentReports?.values?.distinct()?.map { report ->
                IntelligenceBriefingEntity(
                    timestamp = cloud.lastUpdateTimestamp,
                    summary = report,
                    anomalyScore = if (report.contains("SIGNAL_LOST")) 100 else 0,
                    sentiment = "CLOUD_SYNC",
                    riskScore = cloud.macroBriefing?.riskScore ?: 0,
                    evidence = "SOURCE: CLOUD_HARVESTER"
                )
            } ?: emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
