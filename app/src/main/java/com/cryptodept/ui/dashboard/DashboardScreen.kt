package com.cryptodept.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cryptodept.domain.model.CoinPrice
import com.cryptodept.ui.components.TerminalCommandBar
import com.cryptodept.ui.components.PriceText
import com.cryptodept.ui.components.TickerTape
import com.cryptodept.ui.components.TerminalErrorOverlay
import com.cryptodept.ui.components.TerminalLoadingSkeleton
import com.cryptodept.ui.navigation.Screen
import com.cryptodept.ui.theme.*
import com.cryptodept.viewmodel.DashboardUiState
import com.cryptodept.viewmodel.DashboardViewModel
import com.cryptodept.ui.effects.GlitchEffect

@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val networkHealth by viewModel.networkHealth.collectAsState()
    var showHelp by remember { mutableStateOf(false) }

    val colors = LocalTerminalColors.current

    val glitchTrigger = remember(uiState is DashboardUiState.Success) {
        if (uiState is DashboardUiState.Success) "LOAD_COMPLETE" else null
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            containerColor = Color.Black,
            modifier = Modifier.border(1.dp, colors.primary),
            title = { Text("TERMINAL COMMANDS", color = colors.primary, fontFamily = FontFamily.Monospace) },
            text = {
                Column {
                    Text("CHART [SYM] - Open candlestick chart", color = colors.primary, fontFamily = FontFamily.Monospace)
                    Text("NEWS - Global crypto news", color = colors.primary, fontFamily = FontFamily.Monospace)
                    Text("ALERTS - System alerts log", color = colors.primary, fontFamily = FontFamily.Monospace)
                    Text("SETTINGS - Terminal config", color = colors.primary, fontFamily = FontFamily.Monospace)
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) {
                    Text("CLOSE", color = colors.primary, fontFamily = FontFamily.Monospace)
                }
            }
        )
    }

    GlitchEffect(trigger = glitchTrigger) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(8.dp)
        ) {
            val currentPrices = if (uiState is DashboardUiState.Success) (uiState as DashboardUiState.Success).prices else emptyList()
            TickerTape(
                prices = currentPrices,
                networkHealth = networkHealth
            )

            HorizontalDivider(color = colors.grid, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = ">>> MARKET TERMINAL v2.0",
                    color = colors.primary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { navController.navigate(Screen.News.route) }
                )
                Text(
                    text = "SOURCES: MULTI-API",
                    color = AmberPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }

            HorizontalDivider(color = colors.grid, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

            networkHealth?.let { health ->
                NetworkHealthPanel(health) { navController.navigate(Screen.FearGreed.route) }
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

            Box(modifier = Modifier.weight(1f)) {
                when (val state = uiState) {
                    is DashboardUiState.Loading -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(10) {
                                TerminalLoadingSkeleton(Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                    is DashboardUiState.Success -> {
                        Column {
                            Text(
                                "--- SELECT ASSET FOR TECHNICAL ANALYSIS ---",
                                color = colors.dimText,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            MarketList(state.prices) { coinId ->
                                navController.navigate(Screen.CoinDetail.createRoute(coinId))
                            }
                        }
                    }
                    is DashboardUiState.Error -> {
                        TerminalErrorOverlay(message = state.message)
                    }
                }
            }

            TerminalCommandBar(
                onCommandEntered = { cmd ->
                    val parts = cmd.uppercase().split(" ")
                    when (parts[0]) {
                        "HELP" -> showHelp = true
                        "ALERTS" -> navController.navigate(Screen.Alerts.route)
                        "NEWS" -> navController.navigate(Screen.News.route)
                        "SETTINGS" -> navController.navigate(Screen.Settings.route)
                        "CHART" -> if (parts.size > 1) navController.navigate(Screen.Charts.createRoute(parts[1].lowercase()))
                        "ANALYSIS" -> if (parts.size > 1) navController.navigate(Screen.Analysis.createRoute(parts[1].lowercase()))
                        "RISK" -> navController.navigate(Screen.Risk.route)
                        "BRIEF" -> navController.navigate(Screen.Briefing.route)
                        "JOURNAL" -> navController.navigate(Screen.Journal.route)
                    }
                }
            )
        }
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
            fontWeight = FontWeight.Bold
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            QuickAccessButton("RISK", Screen.Risk.route, navController)
            QuickAccessButton("BRIEF", Screen.Briefing.route, navController)
            QuickAccessButton("DERIVS", Screen.Derivatives.route, navController)
            QuickAccessButton("JOURNAL", Screen.Journal.route, navController)
            QuickAccessButton("CAL", Screen.Calendar.route, navController)
            QuickAccessButton("MACRO", Screen.Macro.route, navController)
        }
    }
}

@Composable
fun QuickAccessButton(label: String, route: String, navController: NavController) {
    val colors = LocalTerminalColors.current
    Box(
        modifier = Modifier
            .border(1.dp, colors.primary, RectangleShape)
            .clickable { navController.navigate(route) }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "[$label]",
            color = colors.primary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun NetworkHealthPanel(health: com.cryptodept.domain.model.NetworkHealth, onClick: () -> Unit) {
    val colors = LocalTerminalColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.grid)
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Text(
            text = "SYSTEM NETWORK HEALTH:",
            color = colors.dimText,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            NetworkStat("BTC HASHRATE", health.btcHashrate)
            NetworkStat("ETH GAS", health.ethGas)
            NetworkStat("FEAR/GREED", "${health.fearGreedIndex} (${health.fearGreedLabel.uppercase()})")
        }
    }
}

@Composable
fun MarketDominanceBar(prices: List<CoinPrice>) {
    val colors = LocalTerminalColors.current
    val totalMarketCap = prices.sumOf { it.currentPrice * 1000 }
    val btcCap = prices.find { it.symbol.lowercase() == "btc" }?.let { it.currentPrice * 1000 } ?: 0.0
    val ethCap = prices.find { it.symbol.lowercase() == "eth" }?.let { it.currentPrice * 1000 } ?: 0.0
    val btcDominance = if (totalMarketCap > 0) (btcCap / totalMarketCap).toFloat() else 0.4f
    val ethDominance = if (totalMarketCap > 0) (ethCap / totalMarketCap).toFloat() else 0.2f

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("DOMINANCE: BTC ${String.format("%.1f", btcDominance * 100)}% | ETH ${String.format("%.1f", ethDominance * 100)}%",
            color = colors.dimText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        Row(modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(Color(0xFF1A1A1A))) {
            Box(modifier = Modifier.weight(btcDominance).fillMaxHeight().background(AmberPrimary))
            Box(modifier = Modifier.weight(ethDominance).fillMaxHeight().background(GreenPrimary))
            Box(modifier = Modifier.weight((1f - btcDominance - ethDominance).coerceAtLeast(0f)).fillMaxHeight().background(Color.DarkGray))
        }
    }
}

@Composable
fun MiniHeatmap(prices: List<CoinPrice>) {
    val topPrices = prices.take(10)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        topPrices.forEach { coin ->
            val color = when {
                coin.priceChangePercentage24h > 5 -> Color(0xFF00FF41)
                coin.priceChangePercentage24h > 0 -> Color(0xFF008822)
                coin.priceChangePercentage24h < -5 -> Color(0xFFFF3B30)
                else -> Color(0xFF880000)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(12.dp)
                    .background(color)
            )
        }
    }
}

@Composable
fun NetworkStat(label: String, value: String) {
    val colors = LocalTerminalColors.current
    Column {
        Text(label, color = colors.dimText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = colors.primary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MarketList(prices: List<CoinPrice>, onCoinClick: (String) -> Unit) {
    val colors = LocalTerminalColors.current
    LazyColumn {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("SYMBOL", color = colors.dimText, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                Text("PRICE", color = colors.dimText, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                Text("CHANGE", color = colors.dimText, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
            }
            HorizontalDivider(color = colors.grid, thickness = 0.5.dp)
        }
        items(prices) { coin ->
            CoinRow(coin, onCoinClick)
        }
    }
}

@Composable
fun CoinRow(coin: CoinPrice, onClick: (String) -> Unit) {
    val colors = LocalTerminalColors.current
    val changeColor = if (coin.priceChangePercentage24h >= 0) GreenPrimary else TerminalRed
    val changeSign = if (coin.priceChangePercentage24h >= 0) "+" else ""

    val discrepancyColor = when {
        coin.maxDeviation > 2.0 -> TerminalRed
        coin.maxDeviation > 1.0 -> AmberPrimary
        else -> colors.primary.copy(alpha = 0.6f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, colors.grid, RectangleShape)
            .clickable { onClick(coin.id) }
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = coin.symbol.uppercase(),
                color = AmberPrimary,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
            PriceText(
                price = "$${String.format("%.2f", coin.currentPrice)}",
                fontSize = 14.sp,
                color = colors.primary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$changeSign${String.format("%.2f", coin.priceChangePercentage24h)}%",
                color = changeColor,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "[SOURCES: ${coin.sourcesCount}/5]",
                color = colors.dimText,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            if (coin.maxDeviation > 1.0) {
                Text(
                    text = if (coin.maxDeviation > 2.0) "🔴 UNRELIABLE DATA" else "⚠ PRICE DISCREPANCY",
                    color = discrepancyColor,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}