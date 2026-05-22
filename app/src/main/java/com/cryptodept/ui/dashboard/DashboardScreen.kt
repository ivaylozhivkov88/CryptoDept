package com.cryptodept.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import com.cryptodept.ui.dashboard.components.LiquidationHeatmapStrip
import com.cryptodept.domain.model.CoinPrice
import com.cryptodept.domain.model.AgentStatus
import com.cryptodept.ui.components.StreamingText
import com.cryptodept.ui.tutorial.tutorialTarget
import com.cryptodept.domain.tutorial.TutorialTargetId
import com.cryptodept.ui.components.*
import com.cryptodept.ui.effects.GlitchEffect
import com.cryptodept.ui.components.skeletons.DashboardSkeleton
import com.cryptodept.ui.navigation.Screen
import com.cryptodept.ui.theme.*
import com.cryptodept.util.TerminalConfig
import com.cryptodept.viewmodel.DashboardUiState
import com.cryptodept.viewmodel.DashboardViewModel
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import coil.compose.AsyncImage
import com.cryptodept.ui.effects.shimmerEffect
import com.cryptodept.domain.tier.AccessTier
import com.cryptodept.ui.dashboard.cards.*
import com.cryptodept.domain.tier.FeatureKey
import com.cryptodept.ui.navigation.navigateToPaywall
import com.cryptodept.domain.model.WhaleSignal
import com.cryptodept.util.toCurrency
import com.cryptodept.util.toPercentage
import kotlinx.coroutines.delay
import androidx.compose.foundation.horizontalScroll

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
    val btcChartData by viewModel.btcChartData.collectAsStateWithLifecycle()
    val integrityLogs by viewModel.integrityLogs.collectAsStateWithLifecycle()
    
    val colors = LocalTerminalColors.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth > 600.dp
        
        GlitchEffect(trigger = if (uiState is DashboardUiState.Success) "LOAD" else null) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.background)
                        .padding(horizontal = TerminalConfig.UI.DEFAULT_PADDING)
                        .padding(bottom = TerminalConfig.UI.DEFAULT_PADDING)
                        .imePadding(),
                ) {
                    val currentPrices = if (uiState is DashboardUiState.Success) (uiState as DashboardUiState.Success).prices else emptyList()
                    val liqSummary = if (uiState is DashboardUiState.Success) (uiState as DashboardUiState.Success).liquidationSummary else null

                    // --- 0. NEW LIQUIDATION STRIP (TOP) ---
                    LiquidationHeatmapStrip(
                        summary = liqSummary
                    )
                    
                    Spacer(Modifier.height(4.dp))

                    // --- 0. APP UPDATE BANNER ---
                    activity?.let { AppUpdateBanner(activity = it) }

                    if (broadcastMessage.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .border(0.5.dp, colors.amber.copy(alpha = 0.3f))
                                .background(colors.amber.copy(alpha = 0.05f))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "BROADCAST: $broadcastMessage",
                                color = colors.amber,
                                fontSize = TerminalConfig.UI.FONT_SIZE_SMALL,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // --- ZONE 1: LIVE HEARTBEAT (TICKER) ---
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("MARKET_TICKER", color = colors.dimText, fontSize = TerminalConfig.UI.FONT_SIZE_TINY, fontFamily = FontFamily.Monospace)
                            if (uiState is DashboardUiState.Success) {
                                FreshnessIndicator(lastUpdatedMs = (uiState as DashboardUiState.Success).pricesLastUpdated, label = "PRICES")
                            }
                        }

                        // Single Ticker for Prices
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
                            val alpha by infiniteTransition.animateFloat(
                                initialValue = 0.4f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1500, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "Alpha"
                            )

                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(8.dp) 
                                    .alpha(if (isCloudLive) alpha else 1f)
                                    .background(if (isCloudLive) colors.primary else colors.danger, CircleShape)
                            )
                            Box(modifier = Modifier.weight(1f).height(35.dp).tutorialTarget(TutorialTargetId.DASH_PRICE_TICKER)) {
                                TickerTape(prices = currentPrices, networkHealth = networkHealth, modifier = Modifier.fillMaxSize(), speed = 1.0f)
                            }
                        }
                    }

                    HorizontalDivider(color = colors.grid, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                    if (uiState is DashboardUiState.Error) {
                        TerminalErrorOverlay(message = (uiState as DashboardUiState.Error).message, onRetry = { viewModel.refresh() })
                    } else if (uiState is DashboardUiState.Loading) {
                        DashboardSkeleton(modifier = Modifier.fillMaxSize())
                    } else {
                        val successState = uiState as DashboardUiState.Success
                        
                        // --- ZONE 1.5: HERO ROTATOR (L1) ---
                        HeroPriceRotator(
                            prices = currentPrices,
                            onCoinChanged = { viewModel.onHeroCoinChanged(it) }
                        )

                        Spacer(Modifier.height(8.dp))

                        // --- ZONE 1.6: INTELLIGENCE STRIPS (L2) ---
                        OracleNarrativeStrip(
                            narrative = aiSummary,
                            onExpand = { navController.navigate(Screen.AgentHub.route) }
                        )
                        
                        Spacer(Modifier.height(4.dp))
                        
                        AiPickStrip(
                            symbol = successState.dailyPick?.coinSymbol ?: "BTC",
                            direction = successState.dailyPick?.direction ?: "NEUTRAL",
                            confidence = ((successState.dailyPick?.confidence ?: 0.5f) * 100).toInt(),
                            onExpand = {
                                if (tier == AccessTier.ADMIN) {
                                    navController.navigate(Screen.Prediction.route)
                                } else {
                                    navController.navigateToPaywall("predictions", FeatureKey.PREDICTION_ENGINES_6) 
                                }
                            },
                            onAccuracyClick = { navController.navigate("accuracy_dashboard") }
                        )

                        Spacer(Modifier.height(8.dp))

                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            LazyColumn(modifier = Modifier.weight(if (isWide) 0.6f else 1f).fillMaxHeight()) {
                                
                                // --- ZONE 2: CYCLE CONTEXT (GAUGES) ---
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp, bottom = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                            Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(modifier = Modifier.size(160.dp, 100.dp)) {
                                                networkHealth?.let { FearGreedPieChart3D(value = it.fearGreedIndex.toFloat()) }
                                            }
                                            Spacer(Modifier.width(16.dp))
                                            Box(modifier = Modifier.size(160.dp, 100.dp)) {
                                                macroIntelligence?.let { AltcoinSeasonGauge(value = it.altcoinSeasonIndex.toFloat()) }
                                            }
                                        }
                                    }
                                }

                                if (!isWide) {
                                    // --- ZONE 4: INTELLIGENCE FEED ---
                                    
                                    // 4.2 Top Movers Row (Practical Action)
                                    item {
                                        val movers = currentPrices.sortedByDescending { Math.abs(it.priceChangePercentage24h) }.take(8)
                                        if (movers.isNotEmpty()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "VOLATILITY_SCANNER", 
                                                    color = colors.dimText, 
                                                    fontSize = TerminalConfig.UI.FONT_SIZE_TINY, 
                                                    fontFamily = FontFamily.Monospace,
                                                )
                                                FreshnessIndicator(lastUpdatedMs = successState.pricesLastUpdated, label = "CYCLES")
                                            }
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                contentPadding = PaddingValues(horizontal = 4.dp)
                                            ) {
                                                items(movers) { coin ->
                                                    MoverChip(coin) { navController.navigate(Screen.CoinDetail.createRoute(coin.id)) }
                                                }
                                            }
                                        }
                                    }
                                    
                                    item { Spacer(Modifier.height(32.dp)) }
                                }
                            }

                            if (isWide) {
                                Column(modifier = Modifier.weight(0.4f).padding(start = 16.dp).verticalScroll(rememberScrollState())) {
                                    // SIDE PANEL FOR WIDE SCREENS
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        text = "--- TERMINAL_EXTENSIONS ---",
                                        color = colors.dimText,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    // Other widgets could go here in the future
                                }
                            }
                        }

                        // --- ANCHORED SECTIONS (BOTTOM) ---
                        
                        // 1. Whale Tracker (Now anchored above Agent Status)
                        WhaleCollapsibleSection(
                            signal = successState.whaleSignal, 
                            alerts = successState.cloudWhaleAlerts, 
                            lastUpdatedMs = successState.whaleDataLastUpdated,
                            navController = navController
                        )

                        // 2. Agent Status Line
                        AgentStatusLine(agentStatuses)

                        // 3. System Integrity Logs
                        SystemIntegrityFeed(integrityLogs)
                    }
                }

                // VIGNETTE OVERLAY
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        brush = Brush.radialGradient(
                            0f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.4f),
                            center = center,
                            radius = size.maxDimension / 1.5f
                        )
                    )
                }

                ScanLineOverlay(
                    modifier = Modifier.fillMaxSize(),
                    isScanning = isRefreshing
                )
            }
        }
    }
}

@Composable
fun HeroPriceRotator(
    prices: List<CoinPrice>,
    onCoinChanged: (CoinPrice) -> Unit,
    modifier: Modifier = Modifier
) {
    val favorites = remember(prices) { prices.filter { it.isTracked }.ifEmpty { listOfNotNull(prices.firstOrNull()) } }
    if (favorites.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { favorites.size })
    var isPinned by remember { mutableStateOf(false) }
    val colors = LocalTerminalColors.current

    LaunchedEffect(pagerState.currentPage, favorites) {
        if (favorites.isNotEmpty() && pagerState.currentPage < favorites.size) {
            onCoinChanged(favorites[pagerState.currentPage])
        }
    }

    // Rotation Loop (Auto-swipe)
    if (!isPinned && favorites.size > 1) {
        LaunchedEffect(favorites.size) {
            while (true) {
                delay(30_000)
                if (!pagerState.isScrollInProgress) {
                    val nextStep = (pagerState.currentPage + 1) % favorites.size
                    pagerState.animateScrollToPage(nextStep)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val currentCoin = favorites[page]
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isPinned = !isPinned }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = "https://assets.coingecko.com/coins/images/${currentCoin.id}/small/${currentCoin.id}.png",
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .background(colors.grid.copy(alpha = 0.1f), CircleShape)
                                .shimmerEffect()
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentCoin.symbol.uppercase(),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                if (currentCoin.isTracked) {
                                    Text(
                                        text = " ★",
                                        color = colors.amber,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            Text(
                                text = currentCoin.name.uppercase(),
                                color = colors.dimText,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    
                    if (isPinned) {
                        Text(
                            text = "[ PINNED ]",
                            color = colors.primary,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = currentCoin.currentPrice.toCurrency(),
                        style = terminalTextStyle(
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            glow = true
                        )
                    )
                    
                    val changeColor = if (currentCoin.priceChangePercentage24h >= 0) colors.primary else colors.danger
                    val arrow = if (currentCoin.priceChangePercentage24h >= 0) "▲" else "▼"
                    
                    Text(
                        text = "$arrow ${String.format(Locale.US, "%.2f", currentCoin.priceChangePercentage24h)}%",
                        color = changeColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        if (favorites.size > 1) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                favorites.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .size(width = 12.dp, height = 2.dp)
                            .background(if (index == pagerState.currentPage) colors.primary else colors.grid.copy(alpha = 0.3f))
                    )
                }
            }
        }
    }
}

@Composable
fun OracleNarrativeStrip(narrative: String, onExpand: () -> Unit) {
    val colors = LocalTerminalColors.current
    val isError = narrative.contains("SIGNAL_LOST")
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .border(0.5.dp, if (isError) colors.danger else colors.primary, RectangleShape)
            .clickable { onExpand() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = ">>> SENTINEL",
            color = if (isError) colors.danger else colors.primary,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        
        val statusText = when {
            isError -> "CRITICAL: SIGNAL LOST"
            narrative.contains("VERDICT:") -> narrative.substringAfter("VERDICT:").substringBefore("\n").trim()
            else -> narrative.take(30).plus("...")
        }
        
        Text(
            text = statusText.uppercase(),
            color = Color.White,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
        )
        
        Text(
            text = "[ EXPAND → ]",
            color = if (isError) colors.danger else colors.primary,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun AiPickStrip(symbol: String, direction: String, confidence: Int, onExpand: () -> Unit, onAccuracyClick: () -> Unit) {
    val colors = LocalTerminalColors.current
    val directionColor = when(direction.uppercase()) {
        "BULLISH" -> colors.primary
        "BEARISH" -> colors.danger
        else -> Color.White
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .border(0.5.dp, colors.amber, RectangleShape)
            .clickable { onAccuracyClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = ">>> AI_PICK",
            color = colors.amber,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$symbol ● ",
                color = Color.White,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = direction.uppercase(),
                color = directionColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "  $confidence%",
                color = Color.White,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        
        Text(
            text = "[ TRACK_RECORD → ]",
            color = colors.amber,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.clickable { onExpand() }
        )
    }
}

@Composable
fun SystemIntegrityFeed(logs: List<com.cryptodept.domain.manager.IntegrityLog>) {
    val colors = LocalTerminalColors.current
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp)
            .border(0.5.dp, colors.grid.copy(alpha = 0.3f), RectangleShape)
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { isExpanded = !isExpanded }
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ">>> INTEGRITY_WATCHDOG_LOGS",
                color = colors.dimText,
                fontSize = 7.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isExpanded) "[-]" else "[+]",
                color = colors.primary,
                fontSize = 7.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        if (isExpanded) {
            Spacer(modifier = Modifier.height(4.dp))
            if (logs.isEmpty()) {
                Text(
                    text = "WAITING FOR AGENT-INTEGRITY SCAN...",
                    color = colors.grid,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                // Show last 10 logs when expanded to avoid taking too much space
                logs.takeLast(10).forEach { log ->
                    val timeStr = java.text.SimpleDateFormat("HH:mm:ss", Locale.US).format(java.util.Date(log.timestamp))
                    Text(
                        text = "[$timeStr] ${log.message}",
                        color = if (log.isAnomaly) colors.danger else colors.primary.copy(alpha = 0.7f),
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun MoverChip(coin: CoinPrice, onClick: () -> Unit) {
    val colors = LocalTerminalColors.current
    val color = if (coin.priceChangePercentage24h >= 0) colors.primary else colors.error
    Box(
        modifier = Modifier
            .border(1.dp, color.copy(alpha = 0.3f), RectangleShape)
            .background(color.copy(alpha = 0.05f))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(coin.symbol.uppercase(), color = colors.textPrimary, fontSize = TerminalConfig.UI.FONT_SIZE_TINY, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(
                "${if (coin.priceChangePercentage24h >= 0) "+" else ""}${String.format(Locale.US, "%.1f", coin.priceChangePercentage24h)}%",
                color = color,
                fontSize = TerminalConfig.UI.FONT_SIZE_TINY,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun AgentStatusLine(agentStatuses: Map<String, AgentStatus>) {
    val colors = LocalTerminalColors.current
    val allActive = agentStatuses.values.all { it == AgentStatus.SUCCESS }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(if (allActive) colors.primary else colors.amber, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (allActive) "ALL_INTELLIGENCE_NODES_ACTIVE" else "SYSTEM_SYNCHRONIZING...",
            color = if (allActive) colors.primary.copy(0.6f) else colors.amber.copy(0.6f),
            fontSize = TerminalConfig.UI.FONT_SIZE_TINY,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun WhaleCollapsibleSection(
    signal: WhaleSignal, 
    alerts: List<com.cryptodept.data.remote.model.CloudWhaleAlert>,
    lastUpdatedMs: Long,
    navController: NavController
) {
    val colors = LocalTerminalColors.current
    var isExpanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(0.5.dp, colors.grid)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("WHALE_TRACKER_DATA", color = colors.dimText, fontFamily = FontFamily.Monospace, fontSize = TerminalConfig.UI.FONT_SIZE_TINY)
                FreshnessIndicator(lastUpdatedMs = lastUpdatedMs, label = "FLOW")
            }
            Text(if (isExpanded) "[-]" else "[+]", color = colors.primary, fontFamily = FontFamily.Monospace, fontSize = TerminalConfig.UI.FONT_SIZE_TINY)
        }
        
        if (isExpanded) {
            Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                WhaleInsightCard(
                    signal = signal,
                    onLearnMore = { /* TODO */ },
                    onUpgrade = { navController.navigateToPaywall("whale_tracker", FeatureKey.WHALE_TRACKER) }
                )
                Spacer(Modifier.height(8.dp))
                alerts.take(3).forEach { alert -> CloudWhaleRow(alert) }
            }
        }
    }
}

@Composable
fun CloudWhaleRow(alert: com.cryptodept.data.remote.model.CloudWhaleAlert) {
    val colors = LocalTerminalColors.current
    val timeStr = java.text.SimpleDateFormat("HH:mm", Locale.US).format(java.util.Date(alert.timestamp))
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text(text = "[$timeStr]", color = colors.dimText, fontSize = TerminalConfig.UI.FONT_SIZE_TINY, modifier = Modifier.padding(end = 6.dp))
        Text(
            text = "WHALE: ${(alert.amountUsd/1_000_000.0).toCurrency(1)}M ${alert.asset} -> ${alert.transactionType}",
            color = colors.amber,
            fontSize = TerminalConfig.UI.FONT_SIZE_TINY,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
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
        label = "ALT_SEASON",
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
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.8f),
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 4.dp.toPx()
                val radius = (size.width / 2) - 8.dp.toPx()
                val center = Offset(size.width / 2, size.height - 4.dp.toPx())
                val segments = listOf(colors.danger, colors.danger.copy(alpha = 0.7f), colors.amber, colors.primary.copy(alpha = 0.7f), colors.primary)
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
                val indicatorAngle = 180f + (value / 100f * 180f)
                val rad = Math.toRadians(indicatorAngle.toDouble())
                val dotX = center.x + radius * Math.cos(rad).toFloat()
                val dotY = center.y + radius * Math.sin(rad).toFloat()
                drawCircle(color = Color.Black, radius = 5.dp.toPx(), center = Offset(dotX, dotY))
                drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(dotX, dotY), style = Stroke(width = 1.2.dp.toPx()))
            }
            Column(
                modifier = Modifier.offset(y = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "${value.toInt()}", color = Color.White, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 20.sp)
                Text(text = verdict, color = verdictColor, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(text = label, color = colors.dimText, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AgentSelector(selectedAgentId: String, onAgentSelected: (String) -> Unit) {
    val colors = LocalTerminalColors.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val agents = listOf("SENTINEL", "SCOUT", "PULSE", "QUANT")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        agents.forEach { agent ->
            val isSelected = selectedAgentId == agent
            Text(
                text = "[$agent]",
                color = if (isSelected) (if (agent == "PULSE" || agent == "SCOUT") colors.amber else colors.primary) else colors.dimText,
                fontSize = TerminalConfig.UI.FONT_SIZE_TINY,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                modifier = Modifier.clickable { 
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    onAgentSelected(agent) 
                }
            )
        }
    }
}
