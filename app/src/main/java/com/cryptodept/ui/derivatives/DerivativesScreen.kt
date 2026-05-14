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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.domain.model.FundingRateData
import com.cryptodept.domain.model.LiquidationData
import com.cryptodept.domain.model.OpenInterestData
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.viewmodel.DerivativesUiState
import com.cryptodept.viewmodel.DerivativesViewModel
import java.util.*

@Composable
fun DerivativesScreen(viewModel: DerivativesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val colors = LocalTerminalColors.current

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(16.dp),
    ) {
        when (val uiState = state) {
            is DerivativesUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = colors.primary,
                )
            }
            is DerivativesUiState.Error -> {
                Text(
                    text = ">>> ERROR: ${uiState.message}",
                    color = colors.error,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            is DerivativesUiState.Success -> {
                DerivativesContent(
                    uiState.funding,
                    uiState.openInterest,
                    uiState.liquidations,
                    uiState.heatmap,
                    uiState.magneticZones,
                    onCoinSelect = { viewModel.selectCoin(it) },
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
    heatmap: List<com.cryptodept.domain.model.FundingHeatmapItem>,
    magneticZones: List<com.cryptodept.domain.model.MagneticZone>,
    onCoinSelect: (String) -> Unit,
) {
    val colors = LocalTerminalColors.current
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .border(1.dp, colors.grid, RectangleShape),
    ) {
        item {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = ">>> DERIVATIVES",
                    color = colors.primary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    modifier = Modifier.wrapContentWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("BTC", "ETH", "XRP").forEach { symbol ->
                        Box(
                            modifier = Modifier
                                .border(1.dp, if (funding.symbol == symbol) colors.primary else colors.grid)
                                .clickable { onCoinSelect(symbol) }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = symbol,
                                color = if (funding.symbol == symbol) colors.primary else colors.dimText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = colors.grid, thickness = 1.dp)
        }

        // --- FUNDING RATE ---
        item {
            SectionHeader("FUNDING RATE")
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "EXPLANATION: Periodic payments between longs and shorts. Positive = Longs pay Shorts (Bullish sentiment). Negative = Shorts pay Longs (Bearish sentiment).",
                    color = colors.dimText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Row(modifier = Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("BINANCE:", color = colors.dimText, fontFamily = FontFamily.Monospace)
                    Text(
                        "${String.format(Locale.US, "%.4f", funding.binanceRate)}%",
                        color = colors.textPrimary,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        if (funding.rateLevel.isBullishWarning) "⚠ ELEVATED" else "NORMAL",
                        color = if (funding.rateLevel.isBullishWarning) colors.amber else colors.primary,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                Text(
                    "AGGREGATE: ${String.format(Locale.US, "%.4f", funding.aggregatedRate)}%",
                    color = colors.dimText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                )

                Spacer(modifier = Modifier.height(16.dp))
                // Visual Indicator
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .border(0.5.dp, colors.grid, RectangleShape),
                ) {
                    val normalized = (funding.binanceRate + 0.1) / 0.2 // map -0.1..0.1 to 0..1
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth(normalized.toFloat().coerceIn(0f, 1f))
                                .fillMaxHeight()
                                .background(if (funding.binanceRate > 0.05) colors.error else colors.primary),
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("-0.1%", color = colors.dimText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("0%", color = colors.dimText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text("+0.1%", color = colors.dimText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }
            HorizontalDivider(color = colors.grid, thickness = 1.dp)
        }

        // --- OPEN INTEREST ---
        item {
            SectionHeader("OPEN INTEREST")
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "EXPLANATION: Total number of outstanding derivative contracts. Rising OI with Rising Price = Strong Bullish Trend. Falling OI = Trend exhaustion.",
                    color = colors.dimText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                if (oi != null) {
                    Row(modifier = Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("CURRENT OI:", color = colors.dimText, fontFamily = FontFamily.Monospace)
                        Text(
                            "$${String.format(Locale.US, "%,.1fB", oi.openInterestUsd / 1_000_000_000)}",
                            color = colors.textPrimary,
                            fontFamily = FontFamily.Monospace,
                        )
                        Text(
                            "(${if (oi.openInterestChange24h > 0) "+" else ""}${String.format(
                                Locale.US,
                                "%.1f",
                                oi.openInterestChange24h,
                            )}%)",
                            color = if (oi.openInterestChange24h > 0) colors.primary else colors.error,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    Text(
                        "TREND: ${oi.trend.name.replace("_", " ")}",
                        color = colors.primary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                } else {
                    Text("OI DATA UNAVAILABLE", color = colors.dimText, fontFamily = FontFamily.Monospace)
                }
            }
            HorizontalDivider(color = colors.grid, thickness = 1.dp)
        }

        // --- LIQUIDATIONS ---
        item {
            SectionHeader("LIQUIDATIONS (24H)")
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "EXPLANATION: Forced closing of leveraged positions. High liquidations often mark local bottoms (Longs) or tops (Shorts).",
                    color = colors.dimText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                if (liq != null) {
                    LiquidationBar("LONGS", liq.longLiquidations24h, colors.error)
                    LiquidationBar("SHORTS", liq.shortLiquidations24h, colors.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "DOMINANT: ${liq.dominantSide} GETTING SQUEEZED",
                        color = colors.textPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                    )
                } else {
                    Text("LIQUIDATION DATA UNAVAILABLE", color = colors.dimText, fontFamily = FontFamily.Monospace)
                }
            }
            HorizontalDivider(color = colors.grid, thickness = 1.dp)
        }

        // --- LIQUIDATION LEVELS ---
        item {
            SectionHeader("LIQUIDATION LEVELS")
            Column(modifier = Modifier.padding(8.dp)) {
                liq?.heatmapLevels?.take(5)?.forEach { level ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (level.isSignificant) "⚠ " else "  ",
                            color = colors.amber,
                        )
                        Text(
                            text = "$${String.format(Locale.US, "%,.0f", level.price)}".padEnd(12),
                            color = colors.textPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "— $${String.format(
                                Locale.US,
                                "%,.0fM",
                                (level.longLiquidationUsd + level.shortLiquidationUsd) / 1_000_000,
                            )}",
                            color = if (level.longLiquidationUsd > level.shortLiquidationUsd) colors.error else colors.primary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
            HorizontalDivider(color = colors.grid, thickness = 1.dp)
        }

        // --- FUNDING HEATMAP ---
        item {
            FundingHeatmapSection(heatmap, onCoinSelect)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- MAGNETIC ZONES ---
        item {
            MagneticZonesSection(magneticZones)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun MagneticZonesSection(zones: List<com.cryptodept.domain.model.MagneticZone>) {
    val colors = LocalTerminalColors.current
    SectionHeader("MAGNETIC LIQUIDATION ZONES (PREDICTED)")
    Column(modifier = Modifier.padding(16.dp)) {
        if (zones.isEmpty()) {
            Text(
                "NO SIGNIFICANT ZONES DETECTED",
                color = colors.dimText,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
        zones.forEach { zone ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "$${String.format(Locale.US, "%,.0f", zone.price)}",
                    color = colors.textPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${if (zone.distancePercent > 0) "+" else ""}${String.format(Locale.US, "%.2f", zone.distancePercent)}%",
                    color = if (zone.distancePercent > 0) colors.primary else colors.error,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    text = zone.type.name.replace("_", " "),
                    color = if (zone.type == com.cryptodept.domain.model.LiquidationType.SHORT_SQUEEZE_POTENTIAL) colors.primary else colors.error,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

@Composable
fun FundingHeatmapSection(
    heatmap: List<com.cryptodept.domain.model.FundingHeatmapItem>,
    onCoinSelect: (String) -> Unit,
) {
    val colors = LocalTerminalColors.current
    SectionHeader("FUNDING HEATMAP (BINANCE | BYBIT | OKX)")
    Column(modifier = Modifier.padding(8.dp)) {
        heatmap.forEach { item ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onCoinSelect(item.symbol) }
                        .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.symbol.padEnd(6),
                    color = colors.textPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )

                ExchangeRateCell(item.binanceRate)
                ExchangeRateCell(item.bybitRate)
                ExchangeRateCell(item.okxRate)
            }
        }
    }
}

@Composable
fun ExchangeRateCell(rate: Double) {
    val colors = LocalTerminalColors.current
    val color =
        when {
            rate > 0.1 -> colors.error // Extreme Positive
            rate > 0.05 -> colors.amber
            rate > 0.01 -> colors.amber.copy(alpha = 0.8f)
            rate < -0.05 -> Color(0xFF007AFF) // Extreme Negative (Deep Blue)
            rate < -0.01 -> Color(0xFF5AC8FA)
            else -> colors.primary // Neutral Green
        }

    Box(
        modifier =
            Modifier
                .width(60.dp)
                .background(color.copy(alpha = 0.2f))
                .border(0.5.dp, color, RectangleShape)
                .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "${String.format(Locale.US, "%.3f", rate)}%",
            color = color,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    val colors = LocalTerminalColors.current
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(vertical = 4.dp, horizontal = 8.dp),
    ) {
        Text(
            text = "═ $title " + "═".repeat(15),
            color = colors.primary,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun LiquidationBar(
    label: String,
    amount: Double,
    color: Color,
) {
    val colors = LocalTerminalColors.current
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(label, color = colors.dimText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text(
                "$${String.format(Locale.US, "%,.0fM", amount / 1_000_000)}",
                color = color,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .border(0.5.dp, colors.grid, RectangleShape),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth((amount / 500_000_000).toFloat().coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(color),
            )
        }
    }
}
