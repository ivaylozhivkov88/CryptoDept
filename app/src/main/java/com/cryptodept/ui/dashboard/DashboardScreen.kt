package com.cryptodept.ui.dashboard

import androidx.compose.animation.*
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
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = remember(context) {
        var currentContext = context
        while (currentContext is android.content.ContextWrapper) {
            if (currentContext is android.app.Activity) return@remember currentContext
            currentContext = currentContext.baseContext
        }
        null
    }

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
        activity = activity,
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
    activity: android.app.Activity?,
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

                // 1. [2] MARKET_TICKER (ORDER #1)
                DashboardTickerSection(
                    currentPrices = currentPrices,
                    networkHealth = networkHealth,
                    pricesLastUpdated = successData?.pricesLastUpdated ?: 0L,
                    isCloudLive = isCloudLive,
                    showVerdict = false
                )

                // 2. [9] SYSTEM STATUS BAR (ORDER #2)
                AgentStatusLine(agentStatuses)
                
                Spacer(modifier = Modifier.height(8.dp))

                if (uiState is DashboardUiState.Loading) {
                    DashboardSkeleton(modifier = Modifier.fillMaxSize())
                } else if (successData != null) {
                    
                    // 3. [4] HERO_PRICE_ROTATOR (ORDER #3)
                    HeroPriceRotator(
                        prices = currentPrices,
                        onCoinChanged = onHeroCoinChanged
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4. [7] MARKET_GAUGES (ORDER #4)
                    DashboardMarketOverviewSection(
                        currentPrices = emptyList(),
                        networkHealth = networkHealth,
                        macroIntelligence = macroIntelligence,
                        pricesLastUpdated = successData.pricesLastUpdated,
                        onCoinClick = {}
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 5. [3] GLOBAL_VERDICT (ORDER #5)
                    GlobalVerdictStrip(networkHealth)

                    Spacer(modifier = Modifier.height(8.dp))

                    // 6. [6] AI_PICK STRIP (ORDER #6)
                    AiPickStrip(
                        symbol = successData.dailyPick?.coinSymbol ?: "BTC",
                        direction = successData.dailyPick?.direction ?: "NEUTRAL",
                        confidence = ((successData.dailyPick?.confidence ?: 0.5f) * 100).toInt(),
                        onExpand = { navController.navigate(Screen.Prediction.route) },
                        onAccuracyClick = { navController.navigate("accuracy_dashboard") }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 7. [1] WATCHLIST_OPERATIVES (ORDER #7)
                    DashboardWatchlistSection(
                        currentPrices = currentPrices,
                        onCoinClick = { navController.navigate(Screen.CoinDetail.createRoute(it)) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 8. [5] SENTINEL STRIP (ORDER #8)
                    OracleNarrativeStrip(
                        narrative = aiSummary,
                        onExpand = { navController.navigate("agent_hub") }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // 9. [8] WHALE_TRACKER_DATA (ORDER #9)
                    DashboardWhaleSection(
                        signal = successData.whaleSignal,
                        alerts = successData.cloudWhaleAlerts,
                        lastUpdatedMs = successData.whaleDataLastUpdated,
                        navController = navController,
                        tier = tier
                    )
                    
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
