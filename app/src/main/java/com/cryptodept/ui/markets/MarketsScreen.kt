package com.cryptodept.ui.markets

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.cryptodept.domain.model.CoinPrice
import com.cryptodept.ui.components.EmptyState
import com.cryptodept.ui.components.ErrorState
import com.cryptodept.ui.components.TerminalLoadingSkeleton
import com.cryptodept.ui.tutorial.tutorialTarget
import com.cryptodept.domain.tutorial.TutorialTargetId
import com.cryptodept.ui.navigation.Screen
import com.cryptodept.ui.navigation.navigateToPaywall
import com.cryptodept.ui.components.UpgradeBanner
import com.cryptodept.ui.components.FeatureHelpIcon
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.util.TerminalConfig
import com.cryptodept.viewmodel.MarketsUiState
import com.cryptodept.viewmodel.MarketsViewModel
import androidx.compose.ui.tooling.preview.Preview
import com.cryptodept.ui.theme.CryptoDeptTheme
import java.util.Locale

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun MarketsListPreview() {
    val sampleCoins = listOf(
        CoinPrice(id = "bitcoin", symbol = "BTC", name = "Bitcoin", currentPrice = 64231.5, priceChange24h = 1000.0, priceChangePercentage24h = 1.5, marketCap = 0.0, totalVolume = 0.0, high24h = 0.0, low24h = 0.0, lastUpdated = 0L, isTracked = true),
        CoinPrice(id = "ethereum", symbol = "ETH", name = "Ethereum", currentPrice = 3450.2, priceChange24h = -50.0, priceChangePercentage24h = -2.1, marketCap = 0.0, totalVolume = 0.0, high24h = 0.0, low24h = 0.0, lastUpdated = 0L, isTracked = false),
        CoinPrice(id = "solana", symbol = "SOL", name = "Solana", currentPrice = 145.8, priceChange24h = 10.0, priceChangePercentage24h = 5.7, marketCap = 0.0, totalVolume = 0.0, high24h = 0.0, low24h = 0.0, lastUpdated = 0L, isTracked = true),
        CoinPrice(id = "ripple", symbol = "XRP", name = "Ripple", currentPrice = 0.62, priceChange24h = 0.01, priceChangePercentage24h = 0.5, marketCap = 0.0, totalVolume = 0.0, high24h = 0.0, low24h = 0.0, lastUpdated = 0L, isTracked = false)
    )
    
    val sentimentMap = mapOf(
        "bitcoin" to com.cryptodept.domain.usecase.SentimentVerdict.BULLISH,
        "ethereum" to com.cryptodept.domain.usecase.SentimentVerdict.BEARISH
    )

    CryptoDeptTheme {
        Box(modifier = Modifier.background(Color.Black).padding(16.dp)) {
            MarketsList(
                coins = sampleCoins,
                isProUpgradeNeeded = true,
                sentimentMap = sentimentMap,
                onCoinClick = {},
                onToggleTracking = {},
                onUpgradeClick = {}
            )
        }
    }
}

@Composable
fun MarketsScreen(
    navController: NavHostController,
    viewModel: MarketsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sentimentMap by viewModel.sentimentMap.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val trackedCoins by viewModel.trackedCoins.collectAsStateWithLifecycle()
    val errorEvent by viewModel.errorEvents.collectAsState(initial = null)
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val colors = LocalTerminalColors.current
    
    var showAddOverlay by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showWatchlistLimitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(showAddOverlay) {
        if (showAddOverlay) {
            viewModel.search("")
        }
    }

    LaunchedEffect(errorEvent) {
        if (errorEvent?.contains("Watchlist limit") == true) {
            showWatchlistLimitDialog = true
        }
    }

    if (showWatchlistLimitDialog) {
        AlertDialog(
            onDismissRequest = { showWatchlistLimitDialog = false },
            title = { Text("Watchlist Limit Reached", fontFamily = FontFamily.Monospace, color = colors.primary) },
            text = { 
                val limit = if (viewModel.tierAccessManager.getCachedTier().canAccess(com.cryptodept.domain.tier.AccessTier.PRO)) 30 else 3
                Text(
                    "Current limit for your tier is $limit tracked coins.\n\nUpgrade to Pro for 30 slots or Admin for unlimited capacity.",
                    fontFamily = FontFamily.Monospace,
                    color = colors.textPrimary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showWatchlistLimitDialog = false
                    navController.navigateToPaywall("watchlist", com.cryptodept.domain.tier.FeatureKey.WATCHLISTS_UNLIMITED)
                }) {
                    Text("[ UPGRADE_TO_PRO ]", fontFamily = FontFamily.Monospace, color = colors.amber)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWatchlistLimitDialog = false }) {
                    Text("[ CLOSE ]", fontFamily = FontFamily.Monospace, color = colors.dimText)
                }
            },
            containerColor = colors.background,
            shape = RectangleShape
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(colors.background)
                    .padding(TerminalConfig.UI.DEFAULT_PADDING),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "--- GLOBAL_MARKET_TERMINAL ---",
                    color = colors.primary,
                    fontSize = TerminalConfig.UI.FONT_SIZE_LARGE,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .tutorialTarget(TutorialTargetId.MARKETS_GLOBAL_STATS),
                )
                val successState = uiState as? MarketsUiState.Success
                FeatureHelpIcon(
                    feature = if (successState?.isProUpgradeNeeded == false) com.cryptodept.domain.tier.FeatureKey.MARKETS_TOP_200 else com.cryptodept.domain.tier.FeatureKey.MARKETS_TOP_50,
                    iconSize = 14.dp
                )
            }
            
            Spacer(modifier = Modifier.height(TerminalConfig.UI.SMALL_PADDING))

            when (val state = uiState) {
                is MarketsUiState.Loading -> {
                    LazyColumn {
                        items(10) {
                            TerminalLoadingSkeleton(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
                is MarketsUiState.Success -> {
                    if (state.coins.isEmpty()) {
                        EmptyState(
                            title = "NO_MARKETS_DATA",
                            description = "Market feed is currently empty. Pull to refresh or check your trackers.",
                            actionLabel = "REFRESH_FEED",
                            onAction = { viewModel.refreshData() }
                        )
                    } else {
                        MarketsList(
                            coins = state.coins,
                            isProUpgradeNeeded = state.isProUpgradeNeeded,
                            sentimentMap = sentimentMap,
                            onCoinClick = { coinId ->
                                navController.navigate(Screen.CoinDetail.createRoute(coinId))
                            },
                            onToggleTracking = { coinId ->
                                viewModel.toggleTracking(coinId)
                            },
                            onUpgradeClick = { navController.navigateToPaywall("markets", com.cryptodept.domain.tier.FeatureKey.MARKETS_TOP_200) }
                        )
                    }
                }
                is MarketsUiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.loadMarkets() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(TerminalConfig.UI.SPACER_LARGE))

            // ADD COIN BUTTON (Terminal Refactor)
            Button(
                onClick = { showAddOverlay = true },
                modifier = Modifier.fillMaxWidth().height(TerminalConfig.Interaction.TOUCH_TARGET_SIZE.dp),
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(containerColor = colors.surface, contentColor = colors.primary),
                border = BorderStroke(TerminalConfig.UI.BORDER_WIDTH, colors.primary)
            ) {
                Text("ADD_COIN [ + ]", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        // FULL SCREEN OVERLAY FOR ADDING/MANAGING ASSETS
        if (showAddOverlay) {
            AddAssetOverlay(
                searchQuery = searchQuery,
                onSearchQueryChange = { 
                    searchQuery = it
                    viewModel.search(it)
                },
                searchResults = searchResults,
                trackedCoins = trackedCoins,
                onToggleTracking = { viewModel.toggleTracking(it) },
                onDismiss = { showAddOverlay = false; searchQuery = "" },
                limit = if (viewModel.tierAccessManager.getCachedTier().canAccess(com.cryptodept.domain.tier.AccessTier.PRO)) 30 else 3
            )
        }

        com.cryptodept.ui.components.ScanLineOverlay(
            modifier = Modifier.fillMaxSize(),
            isScanning = isRefreshing
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    val colors = LocalTerminalColors.current
    Text(
        text = title,
        color = colors.amber,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun AddAssetOverlay(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResults: List<CoinPrice>,
    trackedCoins: List<CoinPrice>,
    onToggleTracking: (String) -> Unit,
    onDismiss: () -> Unit,
    limit: Int
) {
    val colors = LocalTerminalColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .clickable(enabled = false) { } // Consume clicks
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().border(1.dp, colors.primary, RectangleShape).padding(16.dp)) {
            Text(">>> TRACKER_MANAGEMENT_PROTOCOL", color = colors.primary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            com.cryptodept.ui.components.TerminalInput(
                label = "SEARCH_GLOBAL_ASSET_NODE",
                value = searchQuery,
                onValueChange = onSearchQueryChange
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            // QUICK SUGGESTIONS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("BTC", "ETH", "SOL", "BNB", "XRP", "ADA", "TON").forEach { sym ->
                    Surface(
                        onClick = { onSearchQueryChange(sym) },
                        color = if (searchQuery.uppercase() == sym) colors.primary.copy(alpha = 0.2f) else colors.surface,
                        shape = RectangleShape,
                        border = BorderStroke(1.dp, if (searchQuery.uppercase() == sym) colors.primary else colors.grid)
                    ) {
                        Text(
                            text = sym,
                            color = if (searchQuery.uppercase() == sym) colors.primary else colors.dimText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                if (searchQuery.isNotEmpty()) {
                    item { SectionHeader("SEARCH_RESULTS") }
                    items(searchResults) { coin ->
                        AssetRow(coin, onToggleTracking, colors)
                    }
                    if (searchResults.isEmpty()) {
                        item { Text("NO_MATCHING_NODES_FOUND", color = colors.danger, fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
                    }
                } else {
                    item { SectionHeader("CURRENT_TRACKERS [ ${trackedCoins.size} / $limit ]") }
                    items(trackedCoins) { coin ->
                        AssetRow(coin, onToggleTracking, colors)
                    }
                    if (trackedCoins.isEmpty()) {
                        item { Text("NO_ACTIVE_TRACKERS", color = colors.dimText, fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.Black)
            ) {
                Text("CLOSE_PROTOCOL", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun AssetRow(coin: CoinPrice, onToggle: (String) -> Unit, colors: com.cryptodept.ui.theme.TerminalColorSet) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(0.5.dp, colors.grid)
            .clickable { onToggle(coin.id) } // Entire row is now clickable for easier management
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(coin.symbol.uppercase(), color = colors.primary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Text(coin.name, color = colors.dimText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
        Text(
            text = if (coin.isTracked) "[ REMOVE - ]" else "[ ADD + ]",
            color = if (coin.isTracked) colors.danger else colors.primary,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MarketsList(
    coins: List<CoinPrice>,
    isProUpgradeNeeded: Boolean,
    sentimentMap: Map<String, com.cryptodept.domain.usecase.SentimentVerdict>,
    onCoinClick: (String) -> Unit,
    onToggleTracking: (String) -> Unit,
    onUpgradeClick: () -> Unit,
) {
    val colors = LocalTerminalColors.current
    LazyColumn(modifier = Modifier.tutorialTarget(TutorialTargetId.MARKETS_LIST)) {
        item {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(colors.surface)
                        .tutorialTarget(TutorialTargetId.MARKETS_SORT_FILTER),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.width(32.dp)) // Space for star icon
                Text(" ASSET", modifier = Modifier.weight(1f), color = colors.dimText, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                Text("PRICE ", modifier = Modifier.weight(1f), color = colors.dimText, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                Text("24H_CHG ", modifier = Modifier.weight(0.8f), color = colors.dimText, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                Text("SENT ", modifier = Modifier.weight(0.5f), color = colors.dimText, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            }
        }
        items(coins) { coin ->
            MarketRow(
                coin = coin,
                onClick = onCoinClick,
                onToggleTracking = onToggleTracking,
                sentiment = sentimentMap[coin.id],
            )
        }
        
        if (isProUpgradeNeeded) {
            item {
                UpgradeBanner(
                    featureName = "Top 200 Markets",
                    description = "Pro tier unlocks 150 more coins plus advanced filters.",
                    requiredTier = "Pro",
                    onUpgradeClick = onUpgradeClick,
                )
            }
        }
    }
}

@Composable
fun MarketRow(
    coin: CoinPrice,
    onClick: (String) -> Unit,
    onToggleTracking: (String) -> Unit,
    sentiment: com.cryptodept.domain.usecase.SentimentVerdict? = null,
) {
    val colors = LocalTerminalColors.current
    val (flashDirection, flashAlpha) = com.cryptodept.ui.components.rememberPriceFlash(coin.currentPrice)
    val isBigMove = kotlin.math.abs(coin.priceChangePercentage24h) >= 2.0
    
    val rowBackground = when {
        isBigMove && flashDirection == com.cryptodept.ui.components.PriceDirection.UP -> colors.primary.copy(alpha = 0.08f * flashAlpha)
        isBigMove && flashDirection == com.cryptodept.ui.components.PriceDirection.DOWN -> colors.error.copy(alpha = 0.08f * flashAlpha)
        else -> Color.Transparent
    }

    val priceTextColor = when {
        flashDirection == com.cryptodept.ui.components.PriceDirection.UP && flashAlpha > 0f -> androidx.compose.ui.graphics.lerp(colors.primary, colors.primary, flashAlpha) // Primary is already green
        flashDirection == com.cryptodept.ui.components.PriceDirection.DOWN && flashAlpha > 0f -> androidx.compose.ui.graphics.lerp(colors.primary, colors.error, flashAlpha)
        else -> colors.primary
    }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(rowBackground)
                .padding(vertical = 6.dp)
                .border(1.dp, colors.primary.copy(alpha = 0.2f))
                .clickable { onClick(coin.id) }
                .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // TRACKING TOGGLE (STAR)
        Text(
            text = if (coin.isTracked) "★" else "☆",
            color = if (coin.isTracked) colors.primary else colors.dimText,
            fontSize = 18.sp,
            modifier =
                Modifier
                    .padding(end = 8.dp)
                    .tutorialTarget(TutorialTargetId.MARKETS_FAVORITES)
                    .clickable { onToggleTracking(coin.id) },
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                coin.symbol.uppercase(), 
                color = colors.primary, 
                fontWeight = FontWeight.Bold, 
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
            Text(
                coin.name.uppercase(), 
                color = colors.dimText, 
                fontSize = 13.sp, 
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = "$${String.format(Locale.US, "%.2f", coin.currentPrice)}",
            modifier = Modifier.weight(1f),
            color = priceTextColor,
            fontFamily = FontFamily.Monospace
        )

        val trendColor = if (coin.priceChangePercentage24h >= 0) colors.primary else colors.error
        Text(
            text = "${if (coin.priceChangePercentage24h >= 0) "+" else ""}${String.format(
                Locale.US,
                "%.2f",
                coin.priceChangePercentage24h,
            )}%",
            modifier = Modifier.weight(0.8f),
            color = trendColor,
            fontFamily = FontFamily.Monospace
        )

        if (sentiment != null) {
            val sentimentColor =
                when (sentiment) {
                    com.cryptodept.domain.usecase.SentimentVerdict.STRONGLY_BULLISH,
                    com.cryptodept.domain.usecase.SentimentVerdict.BULLISH,
                    -> colors.primary
                    com.cryptodept.domain.usecase.SentimentVerdict.STRONGLY_BEARISH,
                    com.cryptodept.domain.usecase.SentimentVerdict.BEARISH,
                    -> colors.error
                    else -> colors.amber
                }
            Text(
                text = sentiment.name.take(4),
                modifier = Modifier.weight(0.5f),
                color = sentimentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        } else {
            Spacer(modifier = Modifier.weight(0.5f))
        }
    }
}
