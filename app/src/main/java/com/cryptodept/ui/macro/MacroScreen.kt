package com.cryptodept.ui.macro

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.domain.model.MacroCorrelation
import com.cryptodept.domain.model.MacroData
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.viewmodel.MacroUiState
import com.cryptodept.viewmodel.MacroViewModel
import java.util.*

@Composable
fun MacroScreen(viewModel: MacroViewModel = hiltViewModel()) {
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
            is MacroUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = colors.primary,
                )
            }
            is MacroUiState.Error -> {
                Text(
                    text = ">>> ERROR: ${uiState.message}",
                    color = colors.error,
                    fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            is MacroUiState.Success -> {
                MacroContent(uiState.data, uiState.correlations)
            }
        }
    }
}

@Composable
fun MacroContent(
    data: MacroData,
    correlations: List<MacroCorrelation>,
) {
    val colors = LocalTerminalColors.current
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .border(1.dp, colors.primary, RectangleShape),
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text(
                    text = ">>> MACRO CORRELATION MONITOR",
                    color = colors.primary,
                    fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
            HorizontalDivider(color = colors.primary, thickness = 1.dp)
        }

        item {
            Column(modifier = Modifier.padding(16.dp)) {
                MacroAssetRow("S&P 500 (SPY)", data.sp500Price, data.sp500Change)
                MacroAssetRow("GOLD (GLD)", data.goldPrice, data.goldChange)
                MacroAssetRow("DXY (UUP)", data.dxyPrice, data.dxyChange)
            }
            HorizontalDivider(color = colors.primary, thickness = 1.dp)
        }

        item {
            SectionHeader("BTC CORRELATIONS (30D)")
            Column(modifier = Modifier.padding(16.dp)) {
                correlations.forEach { corr ->
                    CorrelationRow("BTC / ${corr.asset}", corr.correlation, corr.description)
                }
            }
            HorizontalDivider(color = colors.primary, thickness = 1.dp)
        }

        item {
            SectionHeader("INTERPRETATION")
            Column(modifier = Modifier.padding(16.dp)) {
                val dxyText =
                    if (data.dxyChange < 0) {
                        "DXY is FALLING → Historically BULLISH for BTC (Liquidity expansion)"
                    } else {
                        "DXY is RISING → BEARISH pressure for BTC (Liquidity contraction)"
                    }
                Text(text = dxyText, color = colors.textPrimary, fontFamily = com.cryptodept.ui.theme.JetBrainsMono, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(12.dp))

                val primaryCorr = correlations.maxByOrNull { kotlin.math.abs(it.correlation) }
                primaryCorr?.let {
                    Text(
                        text = "PRIMARY DRIVER: ${it.asset} (${String.format(Locale.US, "%.2f", it.correlation)})",
                        color = colors.primary,
                        fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun MacroAssetRow(
    label: String,
    price: Double,
    change: Double,
) {
    val colors = LocalTerminalColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label.padEnd(15), color = colors.dimText, fontFamily = com.cryptodept.ui.theme.JetBrainsMono, fontSize = 12.sp)
        Text(
            text = "$${String.format(Locale.US, "%.2f", price)}",
            color = colors.textPrimary,
            fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
            fontSize = 12.sp,
        )
        Text(
            text = "${if (change >= 0) "+" else ""}${String.format(Locale.US, "%.2f", change)}%",
            color = if (change >= 0) colors.primary else colors.error,
            fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
            fontSize = 12.sp,
        )
    }
}

@Composable
fun CorrelationRow(
    label: String,
    correlation: Double,
    description: String,
) {
    val colors = LocalTerminalColors.current
    val barColor =
        when {
            correlation > 0.6 -> colors.primary
            correlation < -0.6 -> colors.error
            else -> colors.amber
        }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(text = label, color = colors.dimText, fontSize = 12.sp, fontFamily = com.cryptodept.ui.theme.JetBrainsMono)
            Text(
                text = "${if (correlation >= 0) "+" else ""}${String.format(Locale.US, "%.2f", correlation)}",
                color = barColor,
                fontSize = 12.sp,
                fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .border(0.5.dp, colors.grid, RectangleShape),
        ) {
            // Map -1..1 to 0..1
            val progress = (correlation + 1) / 2
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(progress.toFloat().coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(barColor),
            )
        }
        Text(
            text = description,
            color = colors.dimText,
            fontSize = 10.sp,
            fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    val colors = LocalTerminalColors.current
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(vertical = 4.dp, horizontal = 8.dp),
    ) {
        Text(
            text = "═ $title " + "═".repeat(20),
            color = colors.primary,
            fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
            fontSize = 12.sp,
        )
    }
}
