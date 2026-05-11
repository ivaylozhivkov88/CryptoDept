package com.cryptodept.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.cryptodept.domain.model.CoinPrice
import com.cryptodept.domain.model.AgentStatus
import com.cryptodept.ui.components.*
import com.cryptodept.ui.effects.GlitchEffect
import com.cryptodept.ui.navigation.Screen
import com.cryptodept.ui.theme.*
import com.cryptodept.util.toPercentage
import com.cryptodept.util.TerminalConfig
import com.cryptodept.viewmodel.DashboardUiState
import com.cryptodept.viewmodel.DashboardViewModel
import com.cryptodept.viewmodel.TutorialStep
import java.util.Locale

@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val networkHealth by viewModel.networkHealth.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val aiSummary by viewModel.aiSummary.collectAsStateWithLifecycle()
    val tutorialStep by viewModel.tutorialStep.collectAsStateWithLifecycle()
    val focusModeEnabled by viewModel.focusModeEnabled.collectAsStateWithLifecycle()
    
    val targetRects = remember { mutableStateMapOf<TutorialStep, androidx.compose.ui.geometry.Rect>() }

    val soundService = LocalTerminalAudioManager.current

    LaunchedEffect(uiState) {
        if (uiState is DashboardUiState.Error) {
            soundService?.playAlert()
        }
    }

    var showHelp by remember { mutableStateOf(false) }
    var showVersion by remember { mutableStateOf(false) }
    var showAdviceDialog by remember { mutableStateOf(false) }
    var adviceAction by remember { mutableStateOf("") }
    var adviceExplanation by remember { mutableStateOf("") }
    var adviceGlitchTrigger by remember { mutableStateOf<String?>(null) }

    val colors = LocalTerminalColors.current

    val glitchTrigger =
        remember(uiState is DashboardUiState.Success) {
            if (uiState is DashboardUiState.Success) "LOAD_COMPLETE" else null
        }

    if (showHelp) {
        TerminalHelpDialog(onDismiss = { showHelp = false })
    }

    if (showVersion) {
        AlertDialog(
            onDismissRequest = { showVersion = false },
            containerColor = colors.background,
            modifier = Modifier.border(TerminalConfig.UI.BORDER_WIDTH, colors.primary),
            title = { Text("SYSTEM VERSION INFO", color = colors.primary, fontFamily = FontFamily.Monospace) },
            text = {
                Column {
                    Text("CRYPTODEPT TERMINAL v3.0.4", color = colors.primary, fontFamily = FontFamily.Monospace)
                    Text("BUILD: 2026.04.30.SUPREME", color = colors.primary, fontFamily = FontFamily.Monospace)
                    Text("ENGINE: ENSEMBLE v2.1", color = colors.primary, fontFamily = FontFamily.Monospace)
                    Text("STATUS: OPTIMIZED", color = colors.primary, fontFamily = FontFamily.Monospace)
                }
            },
            confirmButton = {
                TextButton(onClick = { showVersion = false }) {
                    Text("OK", color = colors.primary, fontFamily = FontFamily.Monospace)
                }
            },
        )
    }

    GlitchEffect(trigger = glitchTrigger) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(colors.background)
                    .padding(TerminalConfig.UI.DEFAULT_PADDING),
        ) {
            val currentPrices = if (uiState is DashboardUiState.Success) (uiState as DashboardUiState.Success).prices else emptyList()
            TickerTape(
                prices = currentPrices,
                networkHealth = networkHealth,
            )

            HorizontalDivider(color = colors.grid, thickness = TerminalConfig.UI.BORDER_WIDTH)

            if (focusModeEnabled) {
                DashboardFocusView(
                    uiState = uiState,
                    networkHealth = networkHealth,
                    navController = navController
                )
            } else {
                networkHealth?.let { health ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        SentimentBadge(
                            pulse = health.socialPulse,
                            label = health.socialPulseLabel,
                        )
                    }
                    HorizontalDivider(color = colors.grid, thickness = 1.dp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = ">>> MARKET TERMINAL v3.0",
                            color = colors.primary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            modifier =
                                Modifier
                                    .testTag("TerminalHeader")
                                    .onTargetPositioned { targetRects[TutorialStep.HEADER] = it }
                                    .clickable(
                                        onClickLabel = "View Market News",
                                        onClick = { navController.navigate(Screen.News.route) },
                                    ),
                        )
                        
                        Spacer(modifier = Modifier.height(30.dp))
                        
                        Text(
                            text = "SOURCES: MULTI-API",
                            color = colors.amber,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .padding(end = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        networkHealth?.let { health ->
                            FearGreedPieChart3D(
                                value = health.fearGreedIndex.toFloat(),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // WHAT SHOULD I DO NOW? button
                        GlitchEffect(trigger = adviceGlitchTrigger) {
                            TextButton(
                                onClick = {
                                    // trigger glitch and compute recommendation
                                    adviceGlitchTrigger = System.currentTimeMillis().toString()
                                    viewModel.computeActionRecommendation { action, explanation ->
                                        adviceAction = action
                                        adviceExplanation = explanation
                                        showAdviceDialog = true
                                    }
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = colors.primary),
                                modifier = Modifier.onTargetPositioned { targetRects[TutorialStep.WHATS_NEXT] = it }
                            ) {
                                Text("[WHAT NOW?]", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            }
                        }
                    }
                }

                HorizontalDivider(color = colors.grid, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                networkHealth?.let { health ->
                    NetworkHealthPanel(
                        health = health,
                        modifier = Modifier.onTargetPositioned { targetRects[TutorialStep.NETWORK_HEALTH] = it },
                        onClick = { navController.navigate(Screen.FearGreed.route) }
                    )
                    HorizontalDivider(color = colors.grid, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
                }

                val prices = (uiState as? DashboardUiState.Success)?.prices ?: emptyList()
                if (prices.isNotEmpty()) {
                    MarketDominanceBar(prices)
                    Spacer(modifier = Modifier.height(8.dp))
                    MiniHeatmap(prices)
                    HorizontalDivider(color = colors.grid, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
                }

                QuickAccessPanel(navController)
                HorizontalDivider(color = colors.grid, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                // AI SUMMARY BANNER
                val agentStatuses by viewModel.agentStatuses.collectAsStateWithLifecycle()

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.primary)
                            .background(colors.primary.copy(alpha = 0.05f))
                            .padding(8.dp)
                            .onTargetPositioned { targetRects[TutorialStep.AI_NARRATIVE] = it }
                            .testTag("AiSummaryBanner"),
                ) {
                    Column {
                        // AGENT STATUS BAR
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            agentStatuses.forEach { (id, status) ->
                                val statusColor = when(status) {
                                    AgentStatus.SCANNING -> colors.amber
                                    AgentStatus.SUCCESS -> colors.primary
                                    else -> colors.dimText
                                }
                                val statusText = when(status) {
                                    AgentStatus.SCANNING -> "SCANNING..."
                                    AgentStatus.SUCCESS -> "ACTIVE"
                                    else -> "READY"
                                }
                                Text(
                                    text = "[$id:$statusText]",
                                    color = statusColor,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = "AI_MARKET_NARRATIVE: $aiSummary",
                            color = colors.textPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // TERMINAL EVENT LOG
                Box(modifier = Modifier.weight(2f).border(1.dp, colors.grid).padding(4.dp)) {
                    Column {
                        Text(
                            text = "--- SYSTEM_EVENT_LOG ---",
                            color = colors.dimText,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            items(events, key = { it.id }) { event ->
                                EventLogRow(event)
                            }
                        }
                    }
                }
            }

            TerminalCommandBar(
                modifier = Modifier
                    .onTargetPositioned { targetRects[TutorialStep.COMMAND_BAR] = it }
                    .imePadding(),
                onCommandEntered = { cmd ->
                    val cleanCmd = cmd.trim().removePrefix("/").uppercase()
                    val parts = cleanCmd.split(" ")
                    when (parts[0]) {
                        "HELP", "MAN" -> showHelp = true
                        "TUTORIAL" -> viewModel.startTutorial()
                        "FOCUSMODE" -> {
                            if (parts.size > 1) {
                                when (parts[1]) {
                                    "ON" -> viewModel.setFocusMode(true)
                                    "OFF" -> viewModel.setFocusMode(false)
                                    "TOGGLE" -> viewModel.setFocusMode(!focusModeEnabled)
                                }
                            } else {
                                viewModel.setFocusMode(!focusModeEnabled)
                            }
                        }
                        "ALERTS" -> navController.navigate(Screen.Alerts.route)
                        "NEWS" -> navController.navigate(Screen.News.route)
                        "MATRIX" -> navController.navigate(Screen.Correlation.route)
                        "SETTINGS" -> navController.navigate(Screen.Settings.route)
                        "CHART" ->
                            if (parts.size > 1) {
                                navController.navigate(
                                    Screen.Charts.createRoute(parts[1].lowercase()),
                                )
                            } else {
                                navController.navigate(Screen.Charts.createRoute("bitcoin"))
                            }
                        "ANALYSIS" ->
                            if (parts.size > 1) {
                                navController.navigate(
                                    Screen.Analysis.createRoute(parts[1].lowercase()),
                                )
                            } else {
                                navController.navigate(Screen.Analysis.createRoute("bitcoin"))
                            }
                        "RISK" -> navController.navigate(Screen.Risk.route)
                        "BRIEF" -> navController.navigate(Screen.Briefing.route)
                        "JOURNAL" -> navController.navigate(Screen.Journal.route)
                        "TOOLS" -> navController.navigate(Screen.ToolsHub.route)
                        "PREDICT" -> navController.navigate(Screen.Prediction.route)
                        "PORTFOLIO" -> navController.navigate(Screen.Portfolio.route)
                        "COACH" -> navController.navigate(Screen.AICoach.route)
                        "BACK" -> navController.popBackStack()
                        "VERSION" -> showVersion = true
                        "LOGOUT" -> viewModel.setAdminStatus(false)
                        "CLEAR" -> { /* Visually handled by CommandBar clearing input */ }
                        "SIZER" -> navController.navigate(Screen.PositionSizer.route)
                        "PLANNER" -> navController.navigate(Screen.TradePlanner.route)
                        "ENTRY" -> navController.navigate(Screen.EntryAnalysis.route)
                        "MTF" -> navController.navigate(Screen.MtfAnalysis.route)
                        "PSYCH" -> navController.navigate(Screen.Psychology.route)
                        "DERIVS" -> navController.navigate(Screen.Derivatives.route)
                        "COMPARE" -> {
                            val c1 = if (parts.size > 1) parts[1].lowercase() else "bitcoin"
                            val c2 = if (parts.size > 2) parts[2].lowercase() else "ethereum"
                            // Map short symbols to IDs if needed, but for now simple navigate
                            navController.navigate(Screen.Comparison.createRoute(c1, c2))
                        }
                    }
                },
            )

            if (showAdviceDialog) {
                AlertDialog(
                    onDismissRequest = { showAdviceDialog = false },
                    title = { Text("ACTION RECOMMENDATION", color = colors.primary, fontFamily = FontFamily.Monospace) },
                    text = {
                        Column {
                            Text("Recommendation: $adviceAction", color = colors.textPrimary, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(adviceExplanation, color = colors.dimText, fontFamily = FontFamily.Monospace)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showAdviceDialog = false }) {
                            Text("OK", color = colors.primary)
                        }
                    },
                    containerColor = colors.background,
                    textContentColor = colors.textPrimary,
                )
            }
        }
    }

    tutorialStep?.let { step ->
        ShowcaseOverlay(
            targetCoordinates = targetRects[step],
            text = step.text,
            onNext = { viewModel.nextStep() },
            onSkip = { viewModel.skipTutorial() },
            isLastStep = step == TutorialStep.FINISHED
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickAccessPanel(navController: NavController) {
    val colors = LocalTerminalColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
        Text(
            text = ">>> QUICK ACCESS ENGINE",
            color = colors.dimText,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
             ) {
            QuickAccessButton("SIZER", Screen.PositionSizer.route, navController)
            QuickAccessButton("PLANNER", Screen.TradePlanner.route, navController)
            QuickAccessButton("ENTRY", Screen.EntryAnalysis.route, navController)
            QuickAccessButton("MTF", Screen.MtfAnalysis.route, navController)
            QuickAccessButton("PSYCH", Screen.Psychology.route, navController)
            QuickAccessButton("RISK", Screen.Risk.route, navController)
            QuickAccessButton("DERIVS", Screen.Derivatives.route, navController)
            QuickAccessButton("JOURNAL", Screen.Journal.route, navController)
        }
    }
}

@Composable
fun QuickAccessButton(
    label: String,
    route: String,
    navController: NavController,
) {
    val colors = LocalTerminalColors.current
    val hapticService = com.cryptodept.ui.components.LocalHapticManager.current
    Box(
        modifier =
            Modifier
                .minimumInteractiveComponentSize() // Ensures 48dp touch target
                .border(1.dp, colors.primary, RectangleShape)
                .clickable(
                    onClickLabel = "Open $label",
                    onClick = {
                        hapticService?.lightTick()
                        navController.navigate(route)
                    },
                ).padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = "[$label]",
            color = colors.primary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
fun NetworkHealthPanel(
    health: com.cryptodept.domain.model.NetworkHealth,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = LocalTerminalColors.current
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .border(1.dp, colors.grid)
                .minimumInteractiveComponentSize()
                .clickable(
                    onClickLabel = "View Detailed Network Health",
                    onClick = onClick
                )
                .padding(8.dp)
                .testTag("NetworkHealthPanel"),
    ) {
        Text(
            text = "SYSTEM NETWORK HEALTH:",
            color = colors.dimText,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            NetworkStat("BTC HASHRATE", health.btcHashrate)
            NetworkStat("ETH GAS", health.ethGas)
            NetworkStat("FEAR/GREED", "${health.fearGreedIndex}")
            NetworkStat("SOCIAL PULSE", "${health.socialPulse} (${health.socialPulseLabel.take(4)})")
        }
    }
}

@Composable
fun MarketDominanceBar(prices: List<CoinPrice>) {
    val colors = LocalTerminalColors.current

    val stats by remember(prices) {
        derivedStateOf {
            val totalMarketCap = prices.sumOf { it.currentPrice * 1000 }
            val btcCap = prices.find { it.symbol.lowercase() == "btc" }?.let { it.currentPrice * 1000 } ?: 0.0
            val ethCap = prices.find { it.symbol.lowercase() == "eth" }?.let { it.currentPrice * 1000 } ?: 0.0
            val btcDom = if (totalMarketCap > 0) (btcCap / totalMarketCap).toFloat() else 0.4f
            val ethDom = if (totalMarketCap > 0) (ethCap / totalMarketCap).toFloat() else 0.2f
            Triple(btcDom, ethDom, totalMarketCap)
        }
    }

    val btcDominance = stats.first
    val ethDominance = stats.second

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "DOMINANCE: BTC ${(btcDominance.toDouble() * 100).toPercentage(
                decimals = 1,
            )} | ETH ${(ethDominance.toDouble() * 100).toPercentage(decimals = 1)}",
            color = colors.dimText,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
        )
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(colors.surface),
        ) {
            if (btcDominance > 0f) {
                Box(modifier = Modifier.weight(btcDominance).fillMaxHeight().background(colors.amber))
            }
            if (ethDominance > 0f) {
                Box(modifier = Modifier.weight(ethDominance).fillMaxHeight().background(colors.primary))
            }
            val otherDominance = (1f - btcDominance - ethDominance).coerceAtLeast(0f)
            if (otherDominance > 0f) {
                Box(modifier = Modifier.weight(otherDominance).fillMaxHeight().background(colors.grid))
            }
        }
    }
}

@Composable
fun MiniHeatmap(prices: List<CoinPrice>) {
    val colors = LocalTerminalColors.current
    val topPrices = prices.take(10)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        topPrices.forEach { coin ->
            val color =
                when {
                    coin.priceChangePercentage24h > 5 -> colors.primary
                    coin.priceChangePercentage24h > 0 -> colors.primary.copy(alpha = 0.6f)
                    coin.priceChangePercentage24h < -5 -> colors.danger
                    else -> colors.danger.copy(alpha = 0.6f)
                }
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(12.dp)
                        .background(color),
            )
        }
    }
}

@Composable
fun EventLogRow(event: com.cryptodept.domain.model.SystemEvent) {
    val colors = LocalTerminalColors.current
    val timeStr = java.text.SimpleDateFormat("HH:mm:ss", Locale.US).format(java.util.Date(event.timestamp))

    val color =
        when (event.priority) {
            com.cryptodept.domain.model.EventPriority.CRITICAL -> colors.danger
            com.cryptodept.domain.model.EventPriority.HIGH -> colors.primary
            com.cryptodept.domain.model.EventPriority.MEDIUM -> colors.amber
            else -> colors.dimText
        }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // HEARTBEAT ANIMATION
        if (event.message.contains("SCANNING") || event.message.contains("ANALYZING")) {
            val infiniteTransition = rememberInfiniteTransition(label = "heartbeat")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(colors.primary.copy(alpha = alpha), CircleShape)
                    .padding(end = 4.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        Text(
            text = "[$timeStr]",
            color = colors.dimText,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(end = 6.dp),
        )
        Text(
            text = event.message,
            color = color,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 11.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun FearGreedPieChart3D(
    value: Float,
    modifier: Modifier = Modifier
) {
    val colors = LocalTerminalColors.current
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val sweepAngle = (value / 100f) * 360f
                val remainingAngle = 360f - sweepAngle
                
                val width = size.width
                val height = size.height * 0.7f
                val depth = with(density) { 8.dp.toPx() }
                
                // Side of greed slice (Green)
                drawArc(
                    color = colors.primary.copy(alpha = 0.5f),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, depth),
                    size = androidx.compose.ui.geometry.Size(width, height)
                )
                
                // Side of fear slice (Red)
                drawArc(
                    color = colors.danger.copy(alpha = 0.5f),
                    startAngle = -90f + sweepAngle,
                    sweepAngle = remainingAngle,
                    useCenter = true,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, depth),
                    size = androidx.compose.ui.geometry.Size(width, height)
                )

                // Top greed slice
                drawArc(
                    color = colors.primary,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(width, height)
                )
                
                // Top fear slice
                drawArc(
                    color = colors.danger,
                    startAngle = -90f + sweepAngle,
                    sweepAngle = remainingAngle,
                    useCenter = true,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(width, height)
                )

                // Draw Percentages inside slices
                // Greed %
                if (sweepAngle > 30) {
                    val greedMidAngle = -90f + (sweepAngle / 2f)
                    val rad = Math.toRadians(greedMidAngle.toDouble())
                    val textStr = "${value.toInt()}%"
                    val textLayoutResult = textMeasurer.measure(
                        text = textStr,
                        style = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    )
                    val textX = (width / 2) + (Math.cos(rad) * width / 4).toFloat() - (textLayoutResult.size.width / 2)
                    val textY = (height / 2) + (Math.sin(rad) * height / 4).toFloat() - (textLayoutResult.size.height / 2)
                    
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = androidx.compose.ui.geometry.Offset(textX, textY)
                    )
                }

                // Fear %
                if (remainingAngle > 30) {
                    val fearMidAngle = -90f + sweepAngle + (remainingAngle / 2f)
                    val rad = Math.toRadians(fearMidAngle.toDouble())
                    val textStr = "${100 - value.toInt()}%"
                    val textLayoutResult = textMeasurer.measure(
                        text = textStr,
                        style = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    )
                    val textX = (width / 2) + (Math.cos(rad) * width / 4).toFloat() - (textLayoutResult.size.width / 2)
                    val textY = (height / 2) + (Math.sin(rad) * height / 4).toFloat() - (textLayoutResult.size.height / 2)
                    
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = androidx.compose.ui.geometry.Offset(textX, textY)
                    )
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text("FEAR", color = colors.danger, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            Text("GREED", color = colors.primary, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun NetworkStat(
    label: String,
    value: String,
) {
    val colors = LocalTerminalColors.current
    Column {
        Text(label, color = colors.dimText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = colors.primary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DashboardFocusView(
    uiState: DashboardUiState,
    networkHealth: com.cryptodept.domain.model.NetworkHealth?,
    navController: NavController
) {
    val colors = LocalTerminalColors.current
    val prices = (uiState as? DashboardUiState.Success)?.prices ?: emptyList()
    
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (prices.isNotEmpty()) {
            val btc = prices.find { it.symbol.lowercase() == "btc" }
            btc?.let {
                Text(
                    text = "BTC / USD",
                    color = colors.dimText,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "$${String.format(Locale.US, "%,.2f", it.currentPrice)}",
                    color = colors.primary,
                    fontSize = 48.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${if (it.priceChangePercentage24h >= 0) "+" else ""}${String.format(Locale.US, "%.2f", it.priceChangePercentage24h)}% (24H)",
                    color = if (it.priceChangePercentage24h >= 0) colors.primary else colors.danger,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        networkHealth?.let { health ->
            SentimentBadge(
                pulse = health.socialPulse,
                label = health.socialPulseLabel,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "FEAR_GREED_INDEX: ${health.fearGreedIndex}",
                color = colors.amber,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        }
        
        Spacer(modifier = Modifier.height(64.dp))
        
        Text(
            text = "[ FOCUS_MODE_ACTIVE ]",
            color = colors.grid,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
        Text(
            text = "TYPE 'FOCUSMODE OFF' TO RESTORE FULL TERMINAL",
            color = colors.dimText,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp
        )
    }
}
