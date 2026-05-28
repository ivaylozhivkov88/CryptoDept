package com.cryptodept.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.cryptodept.ui.dashboard.components.*
import com.cryptodept.domain.model.*
import com.cryptodept.ui.components.*
import com.cryptodept.ui.effects.GlitchEffect
import com.cryptodept.ui.components.skeletons.DashboardSkeleton
import com.cryptodept.ui.navigation.Screen
import com.cryptodept.ui.theme.*
import com.cryptodept.viewmodel.DashboardUiState
import com.cryptodept.viewmodel.DashboardViewModel
import com.cryptodept.domain.tier.AccessTier
import com.cryptodept.domain.manager.IntegrityLog
import com.cryptodept.ui.tutorial.tutorialTarget
import com.cryptodept.domain.tutorial.TutorialTargetId

@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tier by viewModel.currentTier.collectAsStateWithLifecycle()
    val networkHealth by viewModel.networkHealth.collectAsStateWithLifecycle()
    val macroIntelligence by viewModel.macroIntelligence.collectAsStateWithLifecycle()
    val aiSummary by viewModel.aiSummary.collectAsStateWithLifecycle()
    val agentStatuses by viewModel.agentStatuses.collectAsStateWithLifecycle()
    val isCloudLive by viewModel.isCloudLive.collectAsStateWithLifecycle()
    val broadcastMessage by viewModel.broadcastMessage.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val integrityLogs by viewModel.integrityLogs.collectAsStateWithLifecycle()

    DashboardContent(
        uiState = uiState,
        tier = tier,
        networkHealth = networkHealth,
        macroIntelligence = macroIntelligence,
        aiSummary = aiSummary,
        agentStatuses = agentStatuses,
        isCloudLive = isCloudLive,
        broadcastMessage = broadcastMessage,
        isRefreshing = isRefreshing,
        integrityLogs = integrityLogs,
        navController = navController,
        onHeroCoinChanged = { viewModel.onHeroCoinChanged(it) }
    )
}

@Composable
fun DashboardContent(
    uiState: DashboardUiState,
    tier: AccessTier,
    networkHealth: NetworkHealth?,
    macroIntelligence: MacroIntelligence?,
    aiSummary: String,
    agentStatuses: Map<String, AgentStatus>,
    isCloudLive: Boolean,
    broadcastMessage: String,
    isRefreshing: Boolean,
    integrityLogs: List<IntegrityLog>,
    navController: NavController,
    onHeroCoinChanged: (CoinPrice) -> Unit
) {
    val colors = LocalTerminalColors.current
    val snackbarHostState = remember { SnackbarHostState() }

    Box(modifier = Modifier.fillMaxSize()) {
        val successData = (uiState as? DashboardUiState.Success)

        GlitchEffect(trigger = if (uiState is DashboardUiState.Success) "LOAD" else null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.background)
                    .padding(horizontal = 16.dp)
            ) {
                val currentPrices = successData?.prices ?: emptyList()

                // 0. BROADCAST
                if (broadcastMessage.isNotEmpty()) {
                    Text(
                        text = "BROADCAST: $broadcastMessage",
                        color = colors.amber,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // 1. MARKET_TICKER
                DashboardTickerSection(
                    currentPrices = currentPrices,
                    networkHealth = networkHealth,
                    pricesLastUpdated = successData?.pricesLastUpdated ?: 0L,
                    isCloudLive = isCloudLive,
                    showVerdict = false
                )

                // 2. SYSTEM STATUS
                AgentStatusLine(
                    statuses = agentStatuses,
                    modifier = Modifier.tutorialTarget(TutorialTargetId.DASH_NETWORK_HEALTH)
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                if (uiState is DashboardUiState.Loading) {
                    DashboardSkeleton(modifier = Modifier.fillMaxSize())
                } else if (successData != null) {
                    
                    HeroPriceRotator(
                        prices = currentPrices,
                        onCoinChanged = onHeroCoinChanged
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DashboardMarketOverviewSection(
                        currentPrices = emptyList(),
                        networkHealth = networkHealth,
                        macroIntelligence = macroIntelligence,
                        pricesLastUpdated = successData.pricesLastUpdated,
                        onCoinClick = {},
                        modifier = Modifier.tutorialTarget(TutorialTargetId.DASH_SENTIMENT_GAUGE)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    GlobalVerdictStrip(networkHealth)

                    Spacer(modifier = Modifier.height(8.dp))

                    AiPickStrip(
                        symbol = successData.dailyPick?.coinSymbol ?: "BTC",
                        direction = successData.dailyPick?.direction ?: "NEUTRAL",
                        confidence = ((successData.dailyPick?.confidence ?: 0.5f) * 100).toInt(),
                        onExpand = { navController.navigate(Screen.Prediction.route) },
                        onAccuracyClick = { navController.navigate("accuracy_dashboard") }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DashboardWatchlistSection(
                        currentPrices = currentPrices,
                        onCoinClick = { navController.navigate(Screen.CoinDetail.createRoute(it)) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OracleNarrativeStrip(
                        narrative = aiSummary,
                        onExpand = { navController.navigate("agent_hub") },
                        modifier = Modifier.tutorialTarget(TutorialTargetId.DASH_AI_NARRATIVE)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    DashboardWhaleSection(
                        signal = successData.whaleSignal,
                        alerts = successData.cloudWhaleAlerts,
                        lastUpdatedMs = successData.whaleDataLastUpdated,
                        navController = navController,
                        tier = tier,
                        modifier = Modifier.tutorialTarget(TutorialTargetId.DASH_WHALE_FEED)
                    )

                    SystemIntegrityFeed(integrityLogs)
                    
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        ScanLineOverlay(
            modifier = Modifier.fillMaxSize(),
            isScanning = isRefreshing
        )
    }
}
