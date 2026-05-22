package com.cryptodept.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.cryptodept.ui.navigation.Screen
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.util.TerminalConfig
import com.cryptodept.viewmodel.UnifiedSearchViewModel
import com.cryptodept.viewmodel.SearchResult
import com.cryptodept.util.toCurrency
import java.util.Locale

@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: UnifiedSearchViewModel = hiltViewModel()
) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val results by viewModel.searchResult.collectAsStateWithLifecycle()
    val colors = LocalTerminalColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(TerminalConfig.UI.DEFAULT_PADDING)
    ) {
        // --- 1. SEARCH HEADER ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, colors.primary)
                .background(colors.primary.copy(alpha = 0.05f))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                modifier = Modifier.size(20.dp),
                tint = colors.primary
            )
            Spacer(Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "SEARCH_TERMINAL...",
                        color = colors.dimText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = { viewModel.updateQuery(it) },
                    textStyle = TextStyle(
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    ),
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = colors.primary
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // --- 2. SEARCH RESULTS ---
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (query.length < 2) {
                item {
                    Text(
                        text = ">>> ENTER MIN 2 CHARACTERS TO INITIALIZE SEARCH SCAN...",
                        color = colors.dimText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else if (results.assets.isEmpty() && results.agents.isEmpty() && results.whaleAlerts.isEmpty() && !isSearching) {
                item {
                    Text(
                        text = ">>> NO MATCHES FOUND IN GLOBAL DATABASE.",
                        color = colors.danger,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                // ASSETS SECTION
                if (results.assets.isNotEmpty()) {
                    item { SectionHeader("ASSETS_IDENTIFIED") }
                    items(results.assets) { coin ->
                        SearchAssetRow(coin) {
                            navController.navigate(Screen.CoinDetail.createRoute(coin.id))
                        }
                    }
                }

                // AGENTS SECTION
                if (results.agents.isNotEmpty()) {
                    item { SectionHeader("INTELLIGENCE_AGENTS") }
                    items(results.agents) { agent ->
                        SearchAgentRow(agent) {
                            navController.navigate(Screen.AgentHub.route)
                        }
                    }
                }

                // WHALE ALERTS SECTION
                if (results.whaleAlerts.isNotEmpty()) {
                    item { SectionHeader("WHALE_FLOW_ALERTS") }
                    items(results.whaleAlerts) { alert ->
                        SearchWhaleRow(alert) {
                            navController.navigate(Screen.WhaleTracker.route)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    val colors = LocalTerminalColors.current
    Text(
        text = ">>> $title",
        color = colors.primary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun SearchAssetRow(coin: com.cryptodept.domain.model.CoinPrice, onClick: () -> Unit) {
    val colors = LocalTerminalColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, colors.grid)
            .clickable { onClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(coin.symbol.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(coin.name, color = colors.dimText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(coin.currentPrice.toCurrency(), color = Color.White, fontFamily = FontFamily.Monospace)
            Text(
                "${if (coin.priceChangePercentage24h >= 0) "+" else ""}${String.format("%.2f", coin.priceChangePercentage24h)}%",
                color = if (coin.priceChangePercentage24h >= 0) colors.primary else colors.danger,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun SearchAgentRow(agent: com.cryptodept.viewmodel.AgentSearchItem, onClick: () -> Unit) {
    val colors = LocalTerminalColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, colors.grid)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).background(colors.primary, androidx.compose.foundation.shape.CircleShape))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(agent.name, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(agent.role, color = colors.dimText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun SearchWhaleRow(alert: com.cryptodept.data.remote.model.CloudWhaleAlert, onClick: () -> Unit) {
    val colors = LocalTerminalColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, colors.grid)
            .clickable { onClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("WHALE: ${alert.asset}", color = colors.amber, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(alert.transactionType, color = colors.dimText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
        Text(
            (alert.amountUsd / 1_000_000.0).toCurrency(1) + "M",
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )
    }
}
