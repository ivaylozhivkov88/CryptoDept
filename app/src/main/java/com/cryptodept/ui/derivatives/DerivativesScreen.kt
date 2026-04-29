package com.cryptodept.ui.derivatives

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.domain.model.FundingRateData
import com.cryptodept.domain.model.LiquidationData
import com.cryptodept.domain.model.OpenInterestData
import com.cryptodept.viewmodel.DerivativesUiState
import com.cryptodept.viewmodel.DerivativesViewModel
import java.util.*

@Composable
fun DerivativesScreen(
    viewModel: DerivativesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        when (val uiState = state) {
            is DerivativesUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF00FF41)
                )
            }
            is DerivativesUiState.Error -> {
                Text(
                    text = ">>> ERROR: ${uiState.message}",
                    color = Color.Red,
                    fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is DerivativesUiState.Success -> {
                DerivativesContent(
                    uiState.funding,
                    uiState.openInterest,
                    uiState.liquidations,
                    onCoinSelect = { viewModel.selectCoin(it) }
                )
            }
        }
    }
}

@Composable
fun DerivativesContent(
    funding: FundingRateData,
    oi: OpenInterestData?,
    liq: LiquidationData?,
    onCoinSelect: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .border(1.dp, Color(0xFF00FF41), RectangleShape)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ">>> DERIVATIVES TERMINAL",
                    color = Color(0xFF00FF41),
                    fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Row {
                    listOf("BTC", "ETH", "XRP").forEach { symbol ->
                        Text(
                            text = "[$symbol]",
                            color = if (funding.symbol == symbol) Color(0xFF00FF41) else Color.Gray,
                            fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clickable { onCoinSelect(symbol) }
                        )
                    }
                }
            }
            HorizontalDivider(color = Color(0xFF00FF41), thickness = 1.dp)
        }

        // --- FUNDING RATE ---
        item {
            SectionHeader("FUNDING RATE")
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("BINANCE:", color = Color.Gray, fontFamily = com.cryptodept.ui.theme.JetBrainsMono)
                    Text(
                        "${String.format(Locale.US, "%.4f", funding.binanceRate)}%",
                        color = Color.White,
                        fontFamily = com.cryptodept.ui.theme.JetBrainsMono
                    )
                    Text(
                        if (funding.rateLevel.isBullishWarning) "⚠ ELEVATED" else "NORMAL",
                        color = if (funding.rateLevel.isBullishWarning) Color(0xFFFFB000) else Color(0xFF00FF41),
                        fontFamily = com.cryptodept.ui.theme.JetBrainsMono
                    )
                }
                Text(
                    "AGGREGATE: ${String.format(Locale.US, "%.4f", funding.aggregatedRate)}%",
                    color = Color.Gray,
                    fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                    fontSize = 12.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                // Visual Indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .border(0.5.dp, Color.Gray, RectangleShape)
                ) {
                    val normalized = (funding.binanceRate + 0.1) / 0.2 // map -0.1..0.1 to 0..1
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(normalized.toFloat().coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(if (funding.binanceRate > 0.05) Color(0xFFFF3B30) else Color(0xFF00FF41))
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("-0.1%", color = Color.Gray, fontSize = 10.sp, fontFamily = com.cryptodept.ui.theme.JetBrainsMono)
                    Text("0%", color = Color.Gray, fontSize = 10.sp, fontFamily = com.cryptodept.ui.theme.JetBrainsMono)
                    Text("+0.1%", color = Color.Gray, fontSize = 10.sp, fontFamily = com.cryptodept.ui.theme.JetBrainsMono)
                }
            }
            HorizontalDivider(color = Color(0xFF00FF41), thickness = 1.dp)
        }

        // --- OPEN INTEREST ---
        item {
            SectionHeader("OPEN INTEREST")
            Column(modifier = Modifier.padding(16.dp)) {
                if (oi != null) {
                    Row(modifier = Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("CURRENT OI:", color = Color.Gray, fontFamily = com.cryptodept.ui.theme.JetBrainsMono)
                        Text(
                            "$${String.format(Locale.US, "%,.1fB", oi.openInterestUsd / 1_000_000_000)}",
                            color = Color.White,
                            fontFamily = com.cryptodept.ui.theme.JetBrainsMono
                        )
                        Text(
                            "(${if (oi.openInterestChange24h > 0) "+" else ""}${String.format(Locale.US, "%.1f", oi.openInterestChange24h)}%)",
                            color = if (oi.openInterestChange24h > 0) Color(0xFF00FF41) else Color(0xFFFF3B30),
                            fontFamily = com.cryptodept.ui.theme.JetBrainsMono
                        )
                    }
                    Text(
                        "TREND: ${oi.trend.name.replace("_", " ")}",
                        color = Color(0xFF00FF41),
                        fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    Text("OI DATA UNAVAILABLE", color = Color.Gray, fontFamily = com.cryptodept.ui.theme.JetBrainsMono)
                }
            }
            HorizontalDivider(color = Color(0xFF00FF41), thickness = 1.dp)
        }

        // --- LIQUIDATIONS ---
        item {
            SectionHeader("LIQUIDATIONS (24H)")
            Column(modifier = Modifier.padding(16.dp)) {
                if (liq != null) {
                    LiquidationBar("LONGS", liq.longLiquidations24h, Color(0xFFFF3B30))
                    LiquidationBar("SHORTS", liq.shortLiquidations24h, Color(0xFF00FF41))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "DOMINANT: ${liq.dominantSide} GETTING SQUEEZED",
                        color = Color.White,
                        fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                        fontSize = 12.sp
                    )
                } else {
                    Text("LIQUIDATION DATA UNAVAILABLE", color = Color.Gray, fontFamily = com.cryptodept.ui.theme.JetBrainsMono)
                }
            }
            HorizontalDivider(color = Color(0xFF00FF41), thickness = 1.dp)
        }

        // --- LIQUIDATION LEVELS ---
        item {
            SectionHeader("LIQUIDATION LEVELS")
            Column(modifier = Modifier.padding(8.dp)) {
                liq?.heatmapLevels?.take(5)?.forEach { level ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (level.isSignificant) "⚠ " else "  ",
                            color = Color(0xFFFFB000)
                        )
                        Text(
                            text = "$${String.format(Locale.US, "%,.0f", level.price)}".padEnd(12),
                            color = Color.White,
                            fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "— $${String.format(Locale.US, "%,.0fM", (level.longLiquidationUsd + level.shortLiquidationUsd) / 1_000_000)}",
                            color = if (level.longLiquidationUsd > level.shortLiquidationUsd) Color(0xFFFF3B30) else Color(0xFF00FF41),
                            fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF111111))
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Text(
            text = "═ $title " + "═".repeat(20),
            color = Color(0xFF00FF41),
            fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun LiquidationBar(label: String, amount: Double, color: Color) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(label, color = Color.Gray, fontSize = 10.sp, fontFamily = com.cryptodept.ui.theme.JetBrainsMono)
            Text("$${String.format(Locale.US, "%,.0fM", amount / 1_000_000)}", color = color, fontSize = 10.sp, fontFamily = com.cryptodept.ui.theme.JetBrainsMono)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .border(0.5.dp, Color.Gray, RectangleShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((amount / 500_000_000).toFloat().coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(color)
            )
        }
    }
}
