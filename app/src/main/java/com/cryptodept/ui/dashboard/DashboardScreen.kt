package com.cryptodept.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.alpha
import com.cryptodept.R
import com.cryptodept.domain.model.CoinPrice
import com.cryptodept.domain.model.AgentStatus
import com.cryptodept.ui.components.StreamingText
import com.cryptodept.ui.tutorial.tutorialTarget
import com.cryptodept.domain.tutorial.TutorialTargetId
import com.cryptodept.ui.components.*
import com.cryptodept.ui.components.AppUpdateBanner
import com.cryptodept.ui.effects.GlitchEffect
import com.cryptodept.ui.components.skeletons.DashboardSkeleton
import com.cryptodept.ui.navigation.Screen
import com.cryptodept.ui.theme.*
import com.cryptodept.util.TerminalConfig
import com.cryptodept.viewmodel.DashboardUiState
import com.cryptodept.viewmodel.DashboardViewModel
import java.util.Locale

import com.cryptodept.domain.tier.AccessTier
import com.cryptodept.ui.dashboard.cards.*
import com.cryptodept.ui.navigation.navigateToPaywall
import com.cryptodept.domain.model.WhaleSignal
import com.cryptodept.domain.usecase.prediction.DailyAIPick
import com.cryptodept.util.toCurrency
import com.cryptodept.util.toPercentage

@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tier by viewModel.currentTier.collectAsStateWithLifecycle()
    val networkHealth by viewModel.networkHealth.collectAsStateWithLifecycle()
    val macroIntelligence by viewModel.macroIntelligence.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val isAiStreaming by viewModel.isAiStreaming.collectAsStateWithLifecycle()
    val aiSummary by viewModel.aiSummary.collectAsStateWithLifecycle()
    val agentStatuses by viewModel.agentStatuses.collectAsStateWithLifecycle()
    val broadcastMessage by viewModel.broadcastMessage.collectAsStateWithLifecycle()
    
    val colors = LocalTerminalColors.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity

    GlitchEffect(trigger = if (uiState is DashboardUiState.Success) "LOAD" else null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(TerminalConfig.UI.DEFAULT_PADDING)
                .imePadding(),
        ) {
            val currentPrices = if (uiState is DashboardUiState.Success) (uiState as DashboardUiState.Success).prices else emptyList()
            
            // --- 0. APP UPDATE BANNER ---
            activity?.let {
                AppUpdateBanner(activity = it)
            }

            if (broadcastMessage.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(1.dp, colors.amber)
                        .background(colors.amber.copy(alpha = 0.1f))
                        .padding(8.dp)
                ) {
                    Text(
                        text = ">>> BROADCAST: $broadcastMessage",
                        color = colors.amber,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // --- 1. PRICE TICKER ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(35.dp)
                        .tutorialTarget(TutorialTargetId.DASH_PRICE_TICKER)
                ) {
                    TickerTape(prices = currentPrices, networkHealth = networkHealth, modifier = Modifier.fillMaxSize())
                }
                FeatureHelpIcon(feature = com.cryptodept.domain.tier.FeatureKey.DASHBOARD_PRICE_TICKER)
            }

            HorizontalDivider(color = colors.grid, thickness = TerminalConfig.UI.BORDER_WIDTH)

            if (uiState is DashboardUiState.Error) {
                TerminalErrorOverlay(message = (uiState as DashboardUiState.Error).message, onRetry = { viewModel.refresh() })
            } else if (uiState is DashboardUiState.Loading) {
                DashboardSkeleton(modifier = Modifier.fillMaxSize())
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    val successState = uiState as? DashboardUiState.Success
                    
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        DashboardMainContent(
                            networkHealth = networkHealth,
                            macroIntelligence = macroIntelligence,
                            navController = navController,
                            currentPrices = currentPrices,
                            tier = tier,
                            dailyPick = successState?.dailyPick
                        )
                    }
                    DashboardFeedContent(
                        events = events,
                        isAiStreaming = isAiStreaming,
                        aiSummary = aiSummary,
                        tier = tier,
                        shortPulse = successState?.shortPulse ?: "",
                        whaleSignal = successState?.whaleSignal ?: WhaleSignal.NEUTRAL,
                        navController = navController,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // --- FOOTER STATUS INDICATORS ---
                    AgentStatusBar(agentStatuses)
                }
            }
        }
    }
}

@Composable
fun DashboardMainContent(
    networkHealth: com.cryptodept.domain.model.NetworkHealth?,
    macroIntelligence: com.cryptodept.domain.model.MacroIntelligence?,
    navController: NavController,
    currentPrices: List<CoinPrice>,
    tier: AccessTier,
    dailyPick: DailyAIPick?
) {
    val colors = LocalTerminalColors.current
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().height(125.dp), // Height for dual gauges
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f).padding(top = 4.dp)) {
                Text(
                    text = stringResource(R.string.dash_header), 
                    color = colors.primary, 
                    fontSize = 14.sp,
                    modifier = Modifier.tutorialTarget(TutorialTargetId.DASH_NAV_DRAWER)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = stringResource(R.string.dash_sources), color = colors.amber, fontSize = 9.sp)
            }

            // --- 2. DUAL RADAR GAUGE (Fear & Greed + Altcoin Season) ---
            Row(
                modifier = Modifier.weight(2f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Fear & Greed
                Box(modifier = Modifier.weight(1f).tutorialTarget(TutorialTargetId.DASH_SENTIMENT_GAUGE)) {
                    networkHealth?.let { health ->
                        FearGreedPieChart3D(value = health.fearGreedIndex.toFloat())
                    }
                }
                
                // Altcoin Season
                Box(modifier = Modifier.weight(1f)) {
                    macroIntelligence?.let { macro ->
                        AltcoinSeasonGauge(value = macro.altcoinSeasonIndex.toFloat())
                    }
                }
            }
        }

        // --- 3. THE PULSE ROW & BLOOD TICKER ---
        if (macroIntelligence != null) {
            MacroPulseRow(macroIntelligence)
            Spacer(modifier = Modifier.height(4.dp))
            LiquidationTicker(macroIntelligence.totalLiquidations1h)
        }

        HorizontalDivider(color = colors.grid, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

        // Daily AI Pick — Always available as a free tool on the dashboard (Task fix)
        if (dailyPick != null) {
            DailyAIPickCard(
                coinSymbol = dailyPick.coinSymbol,
                direction = dailyPick.direction,
                confidencePercent = (dailyPick.confidence * 100).toInt(),
                accuracyPercent = dailyPick.historicalAccuracy?.let { (it * 100).toInt() },
                sampleSize = dailyPick.sampleSize,
                onSeeAllPredictions = { 
                    if (tier == AccessTier.ADMIN) {
                        navController.navigate(Screen.Prediction.route)
                    } else {
                        navController.navigateToPaywall("predictions") 
                    }
                },
            )
            HorizontalDivider(color = colors.grid, thickness = 1.dp, modifier = Modifier.padding(vertical = 2.dp))
        }
    }
}

@Composable
fun DashboardFeedContent(
    events: List<com.cryptodept.domain.model.SystemEvent>,
    isAiStreaming: Boolean,
    aiSummary: String,
    tier: AccessTier,
    shortPulse: String,
    whaleSignal: WhaleSignal,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val colors = LocalTerminalColors.current
    Column(modifier = modifier) {
        // AI Narrative section - Task 2.14: Priority sizing
        when (tier) {
            AccessTier.FREE -> {
                AIPulseShortCard(
                    summary = shortPulse,
                    onUpgrade = { navController.navigateToPaywall("ai_narrative") },
                    modifier = Modifier.weight(1f) // Push event log down
                )
            }
            AccessTier.PRO, AccessTier.ADMIN -> {
                // --- 3. AI NARRATIVE BOX (FULL) ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.5f) // HIGHER PRIORITY (Task 2.14)
                        .border(1.dp, colors.primary)
                        .tutorialTarget(TutorialTargetId.DASH_AI_NARRATIVE),
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = stringResource(R.string.dash_ai_narrative_header), color = colors.primary, fontSize = 7.sp, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.weight(1f))
                            FeatureHelpIcon(feature = com.cryptodept.domain.tier.FeatureKey.DASHBOARD_AI_NARRATIVE_FULL, iconSize = 10.dp)
                        }
                        StreamingText(text = stringResource(R.string.dash_ai_narrative_prefix, aiSummary), isStreaming = isAiStreaming, modifier = Modifier.fillMaxWidth(), fontSize = 10.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Whale section / Event Log - Task 2.14: Lower priority
        when (tier) {
            AccessTier.FREE -> {
                WhaleInsightCard(
                    signal = whaleSignal,
                    onLearnMore = { /* TODO: show educational dialog */ },
                    onUpgrade = { navController.navigateToPaywall("whale_tracker") },
                    modifier = Modifier.weight(0.8f) // Lower priority
                )
            }
            AccessTier.PRO, AccessTier.ADMIN -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // LOWER PRIORITY than AI Narrative
                        .border(1.dp, colors.grid)
                        .padding(4.dp)
                        .tutorialTarget(TutorialTargetId.DASH_WHALE_FEED)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = stringResource(R.string.dash_event_log_header), color = colors.dimText, fontSize = 9.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            FeatureHelpIcon(feature = com.cryptodept.domain.tier.FeatureKey.DASHBOARD_WHALE_FEED_LIVE, iconSize = 10.dp)
                        }
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            items(events) { event -> EventLogRow(event) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventLogRow(event: com.cryptodept.domain.model.SystemEvent) {
    val colors = LocalTerminalColors.current
    val timeStr = java.text.SimpleDateFormat("HH:mm:ss", Locale.US).format(java.util.Date(event.timestamp))
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text(text = "[$timeStr]", color = colors.dimText, fontSize = 9.sp, modifier = Modifier.padding(end = 6.dp))
        Text(text = event.message, color = colors.primary, fontSize = 9.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
fun FearGreedPieChart3D(value: Float, modifier: Modifier = Modifier) {
    val colors = LocalTerminalColors.current
    
    val verdict = when {
        value <= 25 -> "EXTREME FEAR"
        value <= 46 -> "FEAR"
        value <= 54 -> "NEUTRAL"
        value <= 75 -> "GREED"
        else -> "EXTREME GREED"
    }

    val verdictColor = when {
        value <= 46 -> colors.danger
        value >= 55 -> colors.primary
        else -> colors.amber
    }

    SemiCircleGauge(
        value = value,
        label = "SENTIMENT",
        verdict = verdict,
        verdictColor = verdictColor,
        modifier = modifier
    )
}

@Composable
fun AltcoinSeasonGauge(value: Float, modifier: Modifier = Modifier) {
    val colors = LocalTerminalColors.current
    
    val verdict = when {
        value <= 25 -> "BTC SEASON"
        value <= 46 -> "BTC BIAS"
        value <= 54 -> "NEUTRAL"
        value <= 75 -> "ALT BIAS"
        else -> "ALT SEASON"
    }

    val verdictColor = when {
        value <= 46 -> colors.amber
        value >= 55 -> colors.primary
        else -> colors.dimText
    }

    SemiCircleGauge(
        value = value,
        label = "ALT_SEASON INDEX",
        verdict = verdict,
        verdictColor = verdictColor,
        modifier = modifier
    )
}

@Composable
fun SemiCircleGauge(
    value: Float,
    label: String,
    verdict: String,
    verdictColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalTerminalColors.current
    
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            modifier = Modifier.size(width = 110.dp, height = 70.dp), // Increased height for better centering
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 3.5.dp.toPx() 
                val radius = (size.width / 2) - 15.dp.toPx()
                val center = Offset(size.width / 2, size.height - 12.dp.toPx()) // Shifted center up

                // 5 Color Segments (180 degrees total)
                val segments = listOf(
                    colors.danger,
                    colors.danger.copy(alpha = 0.7f),
                    colors.amber,
                    colors.primary.copy(alpha = 0.7f),
                    colors.primary
                )
                
                val startBaseAngle = 180f
                val sweepTotal = 180f
                val segmentSweep = sweepTotal / segments.size
                val gap = 4f
                
                segments.forEachIndexed { i, color ->
                    drawArc(
                        color = color,
                        startAngle = startBaseAngle + (i * segmentSweep) + (gap / 2),
                        sweepAngle = segmentSweep - gap,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Pointer Dot (Black on the line - CMC style)
                val indicatorAngle = 180f + (value / 100f * 180f)
                val rad = Math.toRadians(indicatorAngle.toDouble())
                val dotX = center.x + radius * Math.cos(rad).toFloat()
                val dotY = center.y + radius * Math.sin(rad).toFloat()
                
                drawCircle(
                    color = Color.Black,
                    radius = 4.5.dp.toPx(),
                    center = Offset(dotX, dotY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = Offset(dotX, dotY),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            
            // Central Value (Lowered for better readability - Task fix)
            Column(
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .offset(y = 12.dp), // MOVED DOWN (Task fix)
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${value.toInt()}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp
                )
                Text(
                    text = verdict,
                    color = verdictColor,
                    fontSize = 7.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
        
        Text(
            text = label,
            color = colors.dimText,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun MacroPulseRow(macro: com.cryptodept.domain.model.MacroIntelligence) {
    val colors = LocalTerminalColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MacroPulseItem(label = "BTC_DOM", value = macro.btcDominance.toPercentage(decimals = 1))
        MacroPulseItem(label = "ETH_GAS", value = "${macro.ethGasGwei} GWEI")
        MacroPulseItem(label = "GLOB_CAP", value = (macro.globalMarketCapUsd / 1_000_000_000_000.0).toCurrency(2) + "T")
    }
}

@Composable
fun MacroPulseItem(label: String, value: String) {
    val colors = LocalTerminalColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "$label: ", color = colors.dimText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Text(text = value, color = colors.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun LiquidationTicker(liq: com.cryptodept.domain.model.LiquidationSnapshot) {
    val colors = LocalTerminalColors.current
    val totalM = (liq.totalUsd / 1_000_000.0).toCurrency(1)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, colors.danger.copy(alpha = 0.3f))
            .background(colors.danger.copy(alpha = 0.05f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = ">>> [BLOOD_FEED] ", 
                color = colors.danger, 
                fontSize = 10.sp, 
                fontWeight = FontWeight.Black, 
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "${totalM}M LIQUIDATED (LAST 1H) — LONGS: ${(liq.longsUsd/1_000_000.0).toCurrency(1)}M | SHORTS: ${(liq.shortsUsd/1_000_000.0).toCurrency(1)}M",
                color = colors.textPrimary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun IndicatorLabel(label: String, color: Color, isActive: Boolean) {
    val colors = LocalTerminalColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = if (isActive) color else color.copy(alpha = 0.3f),
            fontSize = 10.sp,
            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
            fontFamily = FontFamily.Monospace
        )
        if (isActive) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(width = 20.dp, height = 2.dp)
                    .background(color)
            )
        }
    }
}

@Composable
fun AgentStatusBar(agentStatuses: Map<String, AgentStatus>) {
    val colors = LocalTerminalColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        agentStatuses.forEach { (name, status) ->
            val indicator = if (status == AgentStatus.SUCCESS) "●" else "○"
            val color = if (status == AgentStatus.SUCCESS) colors.primary else colors.dimText
            Text(
                text = "$indicator $name",
                color = color,
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
            )
        }
    }
}
