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
import com.cryptodept.domain.model.Direction
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.viewmodel.QuantLabUiState
import com.cryptodept.viewmodel.QuantLabViewModel
import java.util.Locale

@Composable
fun CycleScannerScreen(
    initialCoinId: String? = null,
    onBack: () -> Unit,
    viewModel: QuantLabViewModel = hiltViewModel()
) {
    val colors = LocalTerminalColors.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(initialCoinId) {
        val isValidId = initialCoinId != null && !initialCoinId.contains("{")
        if (isValidId && uiState is QuantLabUiState.Idle) {
            viewModel.runFourierScan(initialCoinId)
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
                    title = "FOURIER_CYCLE_SCANNER",
                    onAssetSelected = { viewModel.runFourierScan(it) },
                    onBack = onBack
                )
            }
            is QuantLabUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = colors.primary)
                        Spacer(Modifier.height(16.dp))
                        Text("COMPUTING_FOURIER_TRANSFORMS...", color = colors.primary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                }
            }
            is QuantLabUiState.FourierSuccess -> {
                FourierResultContent(state, onDismiss = { viewModel.reset() })
            }
            is QuantLabUiState.Error -> {
                TerminalErrorView(state.message) { viewModel.reset() }
            }
            else -> { /* Other success states handled in their respective screens */ }
        }
    }
}

@Composable
private fun FourierResultContent(
    state: QuantLabUiState.FourierSuccess,
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
                text = ">>> SCAN_RESULT: ${state.coinId.uppercase()}",
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
                modifier = Modifier
                    .border(0.5.dp, colors.primary)
                    .clickable { onDismiss() }
                    .padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Fourier Specific UI
        Card(
            modifier = Modifier.fillMaxWidth().border(0.5.dp, colors.primary, RectangleShape),
            colors = CardDefaults.cardColors(containerColor = Color.Black)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "DOMINANT_CYCLES_DETECTED",
                    color = colors.amber,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                
                Text(
                    text = "The Fourier engine decomposed the price action into harmonic waves. " +
                           "Current results indicate a potential ${state.vote.direction} bias " +
                           "with a projected target of ${String.format(Locale.US, "$%.2f", state.vote.targetPrice)}.",
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "CYCLE_METRICS:",
            color = colors.dimText,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(8.dp))

        MetricRow("MODEL_CONFIDENCE", "${(state.vote.confidence * 100).toInt()}%")
        MetricRow("WAVE_AMPLITUDE", "HIGH_FIDELITY")
        MetricRow("PHASE_EXTENSION", "ACTIVE")
        MetricRow("HISTORICAL_PACKETS", "${state.history.size}")

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "NOTE: Fourier cycles are non-causal and should be used to identify window of opportunity rather than exact entry points.",
            color = colors.dimText.copy(alpha = 0.5f),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 12.sp
        )
    }
}

@Composable
fun MetricRow(label: String, value: String) {
    val colors = LocalTerminalColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = colors.dimText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TerminalErrorView(message: String, onRetry: () -> Unit) {
    val colors = LocalTerminalColors.current
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("SCAN_FAILED", color = colors.danger, fontWeight = FontWeight.Bold, fontSize = 20.sp, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, color = colors.danger, textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = colors.danger, contentColor = Color.White),
            shape = RectangleShape
        ) {
            Text("RESET_SCANNER", fontFamily = FontFamily.Monospace)
        }
    }
}
