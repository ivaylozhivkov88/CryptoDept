package com.cryptodept.ui.tools

import androidx.activity.compose.BackHandler
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
fun RegressionTunnelScreen(
    initialCoinId: String? = null,
    onBack: () -> Unit,
    viewModel: QuantLabViewModel = hiltViewModel()
) {
    val colors = LocalTerminalColors.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(initialCoinId) {
        val isValidId = initialCoinId != null && !initialCoinId.contains("{")
        if (isValidId && uiState is QuantLabUiState.Idle) {
            viewModel.runRegressionScan(initialCoinId)
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
                    title = "REGRESSION_TUNNEL",
                    onAssetSelected = { viewModel.runRegressionScan(it) },
                    onBack = onBack
                )
            }
            is QuantLabUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = colors.primary)
                        Spacer(Modifier.height(16.dp))
                        Text("MAPPING_EQUILIBRIUM_MEAN...", color = colors.primary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                }
            }
            is QuantLabUiState.RegressionSuccess -> {
                RegressionResultContent(state, onDismiss = { viewModel.reset() })
            }
            is QuantLabUiState.Error -> {
                TerminalErrorView(state.message) { viewModel.reset() }
            }
            else -> {}
        }
    }
}

@Composable
private fun RegressionResultContent(
    state: QuantLabUiState.RegressionSuccess,
    onDismiss: () -> Unit
) {
    val colors = LocalTerminalColors.current
    val currentPrice = state.history.last().close
    val deviation = ((currentPrice - state.vote.targetPrice) / state.vote.targetPrice) * 100
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ">>> REGRESSION: ${state.coinId.uppercase()}",
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

        Text(
            text = "EQUILIBRIUM_MEAN_TARGET:",
            color = colors.dimText,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = String.format(Locale.US, "$%.2f", state.vote.targetPrice),
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "CURRENT_DEVIATION:",
            color = colors.dimText,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "${if (deviation > 0) "+" else ""}${String.format(Locale.US, "%.2f", deviation)}%",
            color = if (Math.abs(deviation) > 2.0) colors.danger else colors.primary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth().border(0.5.dp, colors.grid, RectangleShape),
            colors = CardDefaults.cardColors(containerColor = colors.grid.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val action = when {
                    deviation > 3.0 -> "OVER_EXTENDED: Price is significantly above the mean. High probability of mean-reversion (correction)."
                    deviation < -3.0 -> "UNDER_EXTENDED: Price is significantly below the mean. High probability of a relief bounce."
                    else -> "STABLE_ZONE: Price is trading near its mathematical equilibrium."
                }
                
                Text(
                    text = ">>> STRATEGIC_NOTE:",
                    color = colors.amber,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = action,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "METHOD: Ordinary Least Squares (OLS) Linear Regression with 24-period lookahead. Standard error is used to define tunnel boundaries.",
            color = colors.dimText.copy(alpha = 0.5f),
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 12.sp
        )
    }
}
