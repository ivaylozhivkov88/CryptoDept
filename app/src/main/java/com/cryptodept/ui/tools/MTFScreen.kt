package com.cryptodept.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.cryptodept.domain.model.*
import com.cryptodept.ui.theme.*
import com.cryptodept.viewmodel.MTFUiState
import com.cryptodept.viewmodel.MTFViewModel

@Composable
fun MTFScreen(
    viewModel: MTFViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onGoToDashboard: () -> Unit = {},
    onNavigateToMarkets: () -> Unit = {},
) {
    val colors = LocalTerminalColors.current
    val state by viewModel.state.collectAsState()
    val selectedCoin by viewModel.selectedCoin.collectAsState()
    val trackedCoins by viewModel.trackedCoins.collectAsState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ">>> MULTI-TIMEFRAME ANALYSIS",
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "[TRACK_MORE]",
                color = colors.amber,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.clickable { onNavigateToMarkets() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (trackedCoins.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "[!] NO TRACKED COINS.",
                    color = colors.danger,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    "GO TO DASHBOARD AND SELECT COINS TO TRACK.",
                    color = colors.dimText,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 24.dp),
                )
                Button(
                    onClick = onGoToDashboard,
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.background),
                ) {
                    Text("[GO TO DASHBOARD]")
                }
            }
        } else {
            // COIN SELECTOR
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    val isAllSelected = selectedCoin == "ALL"
                    Box(
                        modifier =
                            Modifier
                                .border(
                                    width = if (isAllSelected) 3.dp else 1.dp,
                                    color = if (isAllSelected) colors.primary else colors.grid,
                                    shape = RectangleShape
                                )
                                .background(if (isAllSelected) colors.primary.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { viewModel.selectCoin("ALL") }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = "ALL",
                            color = if (isAllSelected) colors.primary else colors.dimText,
                            fontSize = 12.sp,
                            fontWeight = if (isAllSelected) FontWeight.ExtraBold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                items(trackedCoins) { coin ->
                    val isSelected = selectedCoin == coin
                    Box(
                        modifier =
                            Modifier
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) colors.primary else colors.grid,
                                    shape = RectangleShape
                                )
                                .background(if (isSelected) colors.primary.copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { viewModel.selectCoin(coin) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = coin.uppercase(),
                            color = if (isSelected) colors.primary else colors.dimText,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(modifier = Modifier.weight(1f)) {
                when (val uiState = state) {
                    is MTFUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = colors.primary)
                        }
                    }
                    is MTFUiState.Success -> {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            MTFContent(uiState.consensus)
                        }
                    }
                    is MTFUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(uiState.message, color = colors.danger, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("< BACK_TO_TOOLS", color = colors.primary, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun MTFContent(consensus: MTFConsensus) {
    val colors = LocalTerminalColors.current

    // Always show table now
    // TABLE HEADER
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, colors.grid)
                .background(colors.grid.copy(alpha = 0.3f))
                .padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        Text("TF", modifier = Modifier.weight(1f), color = colors.dimText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text("TREND", modifier = Modifier.weight(2f), color = colors.dimText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text("RSI", modifier = Modifier.weight(1.5f), color = colors.dimText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text("MACD", modifier = Modifier.weight(1.5f), color = colors.dimText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text("SIGNAL", modifier = Modifier.weight(2f), color = colors.dimText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }

    consensus.timeframes.forEach { tf ->
        TimeframeRow(tf)
        HorizontalDivider(color = colors.grid, thickness = 1.dp)
    }

    Spacer(modifier = Modifier.height(24.dp))
    SummaryBox(consensus)
}

@Composable
fun SummaryBox(consensus: MTFConsensus) {
    val colors = LocalTerminalColors.current
    val consensusColor =
        when (consensus.consensus) {
            OverallSignal.STRONG_BUY -> colors.primary
            OverallSignal.BUY -> colors.primary.copy(alpha = 0.7f)
            OverallSignal.STRONG_SELL -> colors.danger
            OverallSignal.SELL -> colors.danger.copy(alpha = 0.7f)
            else -> colors.amber
        }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, consensusColor)
                .background(consensusColor.copy(alpha = 0.05f))
                .padding(16.dp),
    ) {
        Column {
            Text(
                text = "CONSENSUS: ${consensus.consensus.name.replace("_", " ")}",
                color = consensusColor,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
            )
            Text(
                text = "BIAS: ${consensus.tradingBias}",
                color = consensusColor.copy(alpha = 0.7f),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = consensus.interpretation,
                color = colors.textPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
fun TimeframeRow(tf: TimeframeSignal) {
    val colors = LocalTerminalColors.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            tf.timeframe,
            modifier = Modifier.weight(1f),
            color = colors.amber,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
        )

        // TREND
        val trendColor =
            when {
                tf.trend.name.contains("UP") -> colors.primary
                tf.trend.name.contains("DOWN") -> colors.danger
                else -> colors.amber
            }
        Text(
            text = "${tf.trend.icon} ${tf.trend.name.replace("STRONG_", "S")}",
            modifier = Modifier.weight(2f),
            color = trendColor,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
        )

        // RSI
        val rsiColor =
            when {
                tf.rsi < 30 -> colors.primary
                tf.rsi > 70 -> colors.danger
                else -> colors.dimText
            }
        Text(
            text = String.format("%.1f", tf.rsi),
            modifier = Modifier.weight(1.5f),
            color = rsiColor,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
        )

        // MACD
        val macdColor =
            when (tf.macdSignal) {
                MTFMacdSignal.BULLISH_CROSS, MTFMacdSignal.BULLISH -> colors.primary
                MTFMacdSignal.BEARISH_CROSS, MTFMacdSignal.BEARISH -> colors.danger
                else -> colors.dimText
            }
        Text(
            text = tf.macdSignal.name.take(4),
            modifier = Modifier.weight(1.5f),
            color = macdColor,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
        )

        // SIGNAL BADGE
        val signalColor =
            when (tf.overallSignal) {
                OverallSignal.STRONG_BUY, OverallSignal.BUY -> colors.primary
                OverallSignal.STRONG_SELL, OverallSignal.SELL -> colors.danger
                else -> colors.amber
            }
        Box(
            modifier =
                Modifier
                    .weight(2f)
                    .background(signalColor.copy(alpha = 0.1f))
                    .border(1.dp, signalColor.copy(alpha = 0.5f))
                    .padding(vertical = 2.dp, horizontal = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = tf.overallSignal.name.take(6),
                color = signalColor,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
