package com.cryptodept.ui.tools

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
fun MarketDynamismScreen(
    initialCoinId: String? = null,
    onBack: () -> Unit,
    viewModel: QuantLabViewModel = hiltViewModel()
) {
    val colors = LocalTerminalColors.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(initialCoinId) {
        val isValidId = initialCoinId != null && !initialCoinId.contains("{")
        if (isValidId && uiState is QuantLabUiState.Idle) {
            viewModel.runDynamismScan(initialCoinId)
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
                    title = "MARKET_DYNAMISM",
                    onAssetSelected = { viewModel.runDynamismScan(it) },
                    onBack = onBack
                )
            }
            is QuantLabUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = colors.primary)
                        Spacer(Modifier.height(16.dp))
                        Text("ANALYZING_FRACTAL_DIMENSION...", color = colors.primary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                }
            }
            is QuantLabUiState.DynamismSuccess -> {
                DynamismResultContent(state, onDismiss = { viewModel.reset() })
            }
            is QuantLabUiState.Error -> {
                TerminalErrorView(state.message) { viewModel.reset() }
            }
            else -> {}
        }
    }
}

@Composable
private fun DynamismResultContent(
    state: QuantLabUiState.DynamismSuccess,
    onDismiss: () -> Unit
) {
    val colors = LocalTerminalColors.current
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ">>> DYNAMISM: ${state.coinId.uppercase()}",
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "[ NEW_SCAN ]",
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                modifier = Modifier.border(0.5.dp, colors.primary).clickable { onDismiss() }.padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Hurst Gauge Visual
        Text(
            text = "HURST_EXPONENT (H): ${String.format(Locale.US, "%.3f", state.hurst)}",
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(Modifier.height(16.dp))
        
        TrendGauge(value = state.hurst)

        Spacer(Modifier.height(32.dp))

        // Fractal Info
        Text(
            text = "FRACTAL_DIMENSION (D): ${String.format(Locale.US, "%.3f", state.fractalDim)}",
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(Modifier.height(16.dp))
        
        ComplexityGauge(value = state.fractalDim)

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth().border(0.5.dp, colors.grid, RectangleShape),
            colors = CardDefaults.cardColors(containerColor = colors.grid.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val interpretation = when {
                    state.hurst > 0.65f -> "STRONG_PERSISTENCE: High probability of trend continuation."
                    state.hurst < 0.45f -> "ANTI_PERSISTENT: Market is in a mean-reverting phase."
                    else -> "GAUSSIAN_NOISE: Price action is random; avoid directional bets."
                }
                
                Text(
                    text = ">>> INTERPRETATION:",
                    color = colors.amber,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = interpretation,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            text = "GLOSSARY: H > 0.5 implies trend persistence. D near 1.0 implies low complexity (smooth trend), D near 2.0 implies high noise.",
            color = colors.dimText.copy(alpha = 0.5f),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 12.sp
        )
    }
}

@Composable
fun TrendGauge(value: Float) {
    val colors = LocalTerminalColors.current
    val normalized = (value.coerceIn(0f, 1f))
    
    Column {
        Box(modifier = Modifier.fillMaxWidth().height(12.dp).border(0.5.dp, colors.grid)) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(normalized)
                    .background(if (value > 0.55f) colors.primary else if (value < 0.45f) colors.amber else colors.dimText)
            )
            // Mid point indicator
            Box(modifier = Modifier.fillMaxHeight().width(2.dp).align(Alignment.Center).background(Color.White))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("MEAN_REVERT", color = colors.dimText, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            Text("RANDOM", color = colors.dimText, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            Text("PERSISTENT", color = colors.dimText, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun ComplexityGauge(value: Float) {
    val colors = LocalTerminalColors.current
    val normalized = (value - 1f).coerceIn(0f, 1f)
    
    Column {
        Box(modifier = Modifier.fillMaxWidth().height(12.dp).border(0.5.dp, colors.grid)) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(normalized)
                    .background(if (value < 1.3f) colors.primary else if (value > 1.7f) colors.danger else colors.amber)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("SIMPLE_TREND", color = colors.dimText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text("EXTREME_NOISE", color = colors.dimText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }
}
