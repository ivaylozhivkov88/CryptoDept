package com.cryptodept.ui.performance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.domain.model.PerformanceStats
import com.cryptodept.ui.components.EmptyState
import com.cryptodept.ui.components.TerminalErrorOverlay
import com.cryptodept.ui.components.TerminalLoadingSkeleton
import com.cryptodept.ui.theme.JetBrainsMono
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.viewmodel.PerformanceUiState
import com.cryptodept.viewmodel.PerformanceViewModel
import java.util.Locale

@Composable
fun PerformanceScreen(
    onBack: () -> Unit,
    viewModel: PerformanceViewModel = hiltViewModel(),
) {
    val colors = LocalTerminalColors.current
    val uiState by viewModel.uiState.collectAsState()

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
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = ">>> PERFORMANCE_TRACKER_V1",
                color = colors.primary,
                fontFamily = JetBrainsMono,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onBack) {
                Text("[X]", color = colors.danger, fontFamily = JetBrainsMono)
            }
        }

        HorizontalDivider(color = colors.grid, modifier = Modifier.padding(vertical = 12.dp))

        when (val state = uiState) {
            is PerformanceUiState.Loading -> {
                TerminalLoadingSkeleton(Modifier.fillMaxSize())
            }
            is PerformanceUiState.Success -> {
                PerformanceContent(state.stats, state.aiInsights)
            }
            is PerformanceUiState.Empty -> {
                EmptyState(
                    title = "NO_TRADING_HISTORY",
                    description = "Close at least one trade in your Journal to see performance analytics.",
                    asciiArt = """
                        [ LOGS: EMPTY ]
                        [ DATA: NONE  ]
                    """.trimIndent(),
                    actionLabel = "GO TO JOURNAL",
                    onAction = { /* Navigate to Journal if needed */ }
                )
            }
            is PerformanceUiState.Error -> {
                TerminalErrorOverlay(message = state.message, onRetry = { viewModel.loadPerformance() })
            }
        }
    }
}

@Composable
fun PerformanceContent(
    stats: PerformanceStats,
    aiInsights: String,
) {
    val colors = LocalTerminalColors.current
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
        // AI INSIGHTS BOX
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.primary)
                    .background(colors.primary.copy(alpha = 0.05f))
                    .padding(12.dp),
        ) {
            Column {
                Text(
                    "AI_RISK_MANAGER_FEEDBACK:",
                    color = colors.primary,
                    fontSize = 10.sp,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = aiInsights,
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontFamily = JetBrainsMono,
                    lineHeight = 18.sp,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // KEY STATS GRID
        Text(">>> CORE_METRICS", color = colors.dimText, fontSize = 12.sp, fontFamily = JetBrainsMono)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard("WIN RATE", "${String.format(Locale.US, "%.1f", stats.winRate * 100)}%", Modifier.weight(1f))
            StatCard("PROFIT FACTOR", String.format(Locale.US, "%.2f", stats.profitFactor), Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(
                "TOTAL P&L",
                "$${String.format(Locale.US, "%.2f", stats.totalPnL)}",
                Modifier.weight(1f),
                color = if (stats.totalPnL >= 0) colors.primary else colors.danger,
            )
            StatCard("MAX DRAWDOWN", "$${String.format(Locale.US, "%.2f", stats.maxDrawdown)}", Modifier.weight(1f), color = colors.danger)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // TRADE HISTORY STATS
        Text(">>> TRADE_DISTRIBUTION", color = colors.dimText, fontSize = 12.sp, fontFamily = JetBrainsMono)
        Spacer(modifier = Modifier.height(8.dp))

        MetricRow("TOTAL TRADES", stats.totalTrades.toString())
        MetricRow("WINNING TRADES", stats.winningTrades.toString(), colors.primary)
        MetricRow("LOSING TRADES", stats.losingTrades.toString(), colors.danger)
        MetricRow("AVG WIN", "$${String.format(Locale.US, "%.2f", stats.averageWin)}", colors.primary)
        MetricRow("AVG LOSS", "$${String.format(Locale.US, "%.2f", stats.averageLoss)}", colors.danger)
        MetricRow("AVG DURATION", formatDuration(stats.averageTradeDuration))

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier,
    color: Color = Color.Unspecified,
) {
    val colors = LocalTerminalColors.current
    Box(
        modifier =
            modifier
                .border(1.dp, colors.grid)
                .padding(12.dp),
    ) {
        Column {
            Text(label, color = colors.dimText, fontSize = 9.sp, fontFamily = JetBrainsMono)
            Text(
                value,
                color = if (color == Color.Unspecified) colors.primary else color,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                fontFamily = JetBrainsMono,
            )
        }
    }
}

@Composable
fun MetricRow(
    label: String,
    value: String,
    color: Color = Color.Unspecified,
) {
    val colors = LocalTerminalColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = colors.dimText, fontSize = 12.sp, fontFamily = JetBrainsMono)
        Text(
            value,
            color = if (color == Color.Unspecified) colors.textPrimary else color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = JetBrainsMono,
        )
    }
}

fun formatDuration(ms: Long): String {
    val hours = ms / (1000 * 60 * 60)
    return if (hours > 24) "${hours / 24}d ${hours % 24}h" else "${hours}h"
}
