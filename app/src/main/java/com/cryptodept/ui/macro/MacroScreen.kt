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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.domain.model.MacroCorrelation
import com.cryptodept.domain.model.MacroData
import com.cryptodept.viewmodel.MacroUiState
import com.cryptodept.viewmodel.MacroViewModel
import java.util.*

@Composable
fun MacroScreen(viewModel: MacroViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp),
    ) {
        when (val uiState = state) {
            is MacroUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF00FF41),
                )
            }
            is MacroUiState.Error -> {
                Text(
                    text = ">>> ERROR: ${uiState.message}",
                    color = Color.Red,
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
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .border(1.dp, Color(0xFF00FF41), RectangleShape),
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Text(
                    text = ">>> MACRO CORRELATION MONITOR",
                    color = Color(0xFF00FF41),
                    fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
            HorizontalDivider(color = Color(0xFF00FF41), thickness = 1.dp)
        }

        item {
            Column(modifier = Modifier.padding(16.dp)) {
                MacroAssetRow("S&P 500 (SPY)", data.sp500Price, data.sp500Change)
                MacroAssetRow("GOLD (GLD)", data.goldPrice, data.goldChange)
                MacroAssetRow("DXY (UUP)", data.dxyPrice, data.dxyChange)
            }
            HorizontalDivider(color = Color(0xFF00FF41), thickness = 1.dp)
        }

        item {
            SectionHeader("BTC CORRELATIONS (30D)")
            Column(modifier = Modifier.padding(16.dp)) {
                correlations.forEach { corr ->
                    CorrelationRow("BTC / ${corr.asset}", corr.correlation, corr.description)
                }
            }
            HorizontalDivider(color = Color(0xFF00FF41), thickness = 1.dp)
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
                Text(text = dxyText, color = Color.White, fontFamily = com.cryptodept.ui.theme.JetBrainsMono, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(12.dp))

                val primaryCorr = correlations.maxByOrNull { kotlin.math.abs(it.correlation) }
                primaryCorr?.let {
                    Text(
                        text = "PRIMARY DRIVER: ${it.asset} (${String.format(Locale.US, "%.2f", it.correlation)})",
                        color = Color(0xFF00FF41),
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
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label.padEnd(15), color = Color.Gray, fontFamily = com.cryptodept.ui.theme.JetBrainsMono, fontSize = 12.sp)
        Text(
            text = "$${String.format(Locale.US, "%.2f", price)}",
            color = Color.White,
            fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
            fontSize = 12.sp,
        )
        Text(
            text = "${if (change >= 0) "+" else ""}${String.format(Locale.US, "%.2f", change)}%",
            color = if (change >= 0) Color(0xFF00FF41) else Color(0xFFFF3B30),
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
    val barColor =
        when {
            correlation > 0.6 -> Color(0xFF00FF41)
            correlation < -0.6 -> Color(0xFFFF3B30)
            else -> Color(0xFFFFB000)
        }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(text = label, color = Color.Gray, fontSize = 12.sp, fontFamily = com.cryptodept.ui.theme.JetBrainsMono)
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
                    .border(0.5.dp, Color.Gray, RectangleShape),
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
            color = Color.Gray,
            fontSize = 10.sp,
            fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF111111))
                .padding(vertical = 4.dp, horizontal = 8.dp),
    ) {
        Text(
            text = "═ $title " + "═".repeat(20),
            color = Color(0xFF00FF41),
            fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
            fontSize = 12.sp,
        )
    }
}
