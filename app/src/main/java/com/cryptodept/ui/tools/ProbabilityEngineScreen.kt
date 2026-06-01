package com.cryptodept.ui.tools

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.viewmodel.QuantLabUiState
import com.cryptodept.viewmodel.QuantLabViewModel
import java.util.Locale

@Composable
fun ProbabilityEngineScreen(
    initialCoinId: String? = null,
    onBack: () -> Unit,
    viewModel: QuantLabViewModel = hiltViewModel()
) {
    val colors = LocalTerminalColors.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(initialCoinId) {
        val isValidId = initialCoinId != null && !initialCoinId.contains("{")
        if (isValidId && uiState is QuantLabUiState.Idle) {
            viewModel.runMonteCarloScan(initialCoinId)
        }
    }

    BackHandler {
        if (uiState !is QuantLabUiState.Idle) {
            viewModel.reset()
        } else {
            onBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when (val state = uiState) {
            is QuantLabUiState.Idle -> {
                QuantAssetSelector(
                    title = "PROBABILITY_ENGINE",
                    onAssetSelected = { viewModel.runMonteCarloScan(it) },
                    onBack = onBack
                )
            }
            is QuantLabUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = colors.primary)
                        Spacer(Modifier.height(16.dp))
                        Text("RUNNING_1000_SIMULATIONS...", color = colors.primary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                }
            }
            is QuantLabUiState.MonteCarloSuccess -> {
                MonteCarloResultContent(state, onDismiss = { viewModel.reset() })
            }
            is QuantLabUiState.Error -> {
                TerminalErrorView(state.message) { viewModel.reset() }
            }
            else -> {}
        }
    }
}

@Composable
private fun MonteCarloResultContent(
    state: QuantLabUiState.MonteCarloSuccess,
    onDismiss: () -> Unit
) {
    val colors = LocalTerminalColors.current
    val dist = state.distribution
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ">>> SIM_RESULT: ${state.coinId.uppercase()}",
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "[ NEW_SIM ]",
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                modifier = Modifier.border(0.5.dp, colors.primary).clickable { onDismiss() }.padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "PRICE_PROBABILITY_DISTRIBUTION (24H):",
            color = colors.amber,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Distribution Table
        ProbabilityRow("90th_PERCENTILE (BULLISH_CASE)", dist.percentile90)
        ProbabilityRow("75th_PERCENTILE", dist.percentile75)
        ProbabilityRow("50th_PERCENTILE (EXPECTED_VALUE)", dist.percentile50)
        ProbabilityRow("25th_PERCENTILE", dist.percentile25)
        ProbabilityRow("10th_PERCENTILE (BEARISH_CASE)", dist.percentile10)

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth().border(0.5.dp, colors.grid, RectangleShape),
            colors = CardDefaults.cardColors(containerColor = colors.grid.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "STATISTICAL_SKEWNESS: ${String.format(Locale.US, "%.4f", dist.skewness)}",
                    color = colors.dimText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(8.dp))
                val skewText = if (dist.skewness > 0) "Positive (Bullish bias)" else "Negative (Bearish bias)"
                Text(
                    text = "Engine indicates a $skewText in the simulation cloud. Cluster analysis confirms ${state.vote.direction} momentum.",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "METHODOLOGY: Geometric Brownian Motion (GBM) via Monte Carlo method. Standard deviation is calculated from the last 30-day volatility index.",
            color = colors.dimText.copy(alpha = 0.5f),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 12.sp
        )
    }
}

@Composable
fun ProbabilityRow(label: String, value: Double) {
    val colors = LocalTerminalColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = colors.dimText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text(String.format(Locale.US, "$%.2f", value), color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}
