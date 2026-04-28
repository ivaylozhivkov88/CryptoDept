package com.cryptodept.ui.analysis

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
import com.cryptodept.domain.model.*
import com.cryptodept.ui.components.TerminalErrorOverlay
import com.cryptodept.ui.components.TerminalLoadingSkeleton
import com.cryptodept.ui.theme.*
import com.cryptodept.viewmodel.AnalysisUiState
import com.cryptodept.viewmodel.AnalysisViewModel
import com.cryptodept.ui.prediction.AnalysisLoadingScreen
import com.cryptodept.ui.prediction.DeepAnalysisResultScreen
import com.cryptodept.ui.prediction.AnalysisUiState as PredictionState
import com.cryptodept.ui.prediction.PredictionViewModel
import java.util.Locale

@Composable
fun AnalysisScreen(
    coinId: String,
    viewModel: AnalysisViewModel = hiltViewModel(),
    predictionViewModel: PredictionViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {} // Добавяме callback за навигация
) {
    LaunchedEffect(coinId) {
        viewModel.loadAnalysis(coinId)
    }

    val state by viewModel.analysisState.collectAsState()
    val predictionState by predictionViewModel.uiState.collectAsState()
    val selectedCoinFromVm = (state as? AnalysisUiState.Success)?.coinId ?: coinId

    when (val pState = predictionState) {
        is PredictionState.Loading -> {
            AnalysisLoadingScreen(state = pState)
            return
        }
        is PredictionState.Success -> {
            Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                TextButton(
                    onClick = { predictionViewModel.reset() },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text("< [BACK_TO_TERMINAL]", color = WallStreetGreen, fontFamily = FontFamily.Monospace)
                }

                Box(modifier = Modifier.weight(1f)) {
                    DeepAnalysisResultScreen(pState.prediction)
                }
            }
            return
        }
        is PredictionState.Error -> {
            TerminalErrorOverlay(
                message = pState.message,
                onRetry = { predictionViewModel.reset() }
            )
            return
        }
        else -> { /* Idle */ }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CRTBlack)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // --- ОБНОВЕНА ЗАГЛАВНА ЧАСТ С БУТОН ЗА НАСТРОЙКИ ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val displayId = if (selectedCoinFromVm.startsWith("{")) "NO_ASSET" else selectedCoinFromVm.uppercase()
            Text(
                text = ">>> TECH_ANALYSIS_V2: $displayId",
                color = WallStreetGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )

            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "System Settings",
                    tint = WallStreetGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        HorizontalDivider(color = GridGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

        // Асет селектор
        val assets = listOf("BTC", "ETH", "XRP", "SOL", "ADA", "DOT", "DOGE", "LINK", "LTC", "AVAX")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            assets.forEach { asset ->
                val isSelected = selectedCoinFromVm.uppercase().contains(asset)
                Box(
                    modifier = Modifier
                        .border(1.dp, if (isSelected) WallStreetGreen else GridGray, RectangleShape)
                        .background(if (isSelected) WallStreetGreen.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { viewModel.loadAnalysis(asset) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(asset, color = WallStreetGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        when (val uiState = state) {
            is AnalysisUiState.Loading -> {
                repeat(5) { TerminalLoadingSkeleton(Modifier.padding(vertical = 8.dp)) }
            }
            is AnalysisUiState.Success -> {
                AnalysisContentV2(uiState, predictionViewModel)
            }
            is AnalysisUiState.Error -> {
                TerminalErrorOverlay(message = uiState.message, onRetry = { viewModel.loadAnalysis(coinId) })
            }
        }
    }
}

@Composable
fun AnalysisContentV2(
    state: AnalysisUiState.Success,
    predictionViewModel: PredictionViewModel
) {
    val signal = state.compositeSignal
    val signalColor = when (signal.strength) {
        SignalStrength.STRONG_BUY -> WallStreetGreen
        SignalStrength.BUY -> WallStreetGreen.copy(alpha = 0.7f)
        SignalStrength.STRONG_SELL -> WallStreetRed
        SignalStrength.SELL -> WallStreetRed.copy(alpha = 0.7f)
        else -> WallStreetAmber
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, signalColor, RectangleShape)
            .background(signalColor.copy(alpha = 0.1f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = signal.strength.name.replace("_", " "),
                color = signalColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "CONFIDENCE: ${String.format(Locale.US, "%.0f", signal.confidence * 100)}%",
                color = signalColor.copy(alpha = 0.7f),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // INDICATORS MATRIX
    Text(">>> INDICATOR_MATRIX", color = TextGray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    state.compositeSignal.indicators.forEach { ind ->
        val indColor = when (ind.sentiment) {
            Sentiment.BULLISH -> WallStreetGreen
            Sentiment.BEARISH -> WallStreetRed
            else -> WallStreetAmber
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(ind.name, color = WallStreetAmber, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
            Text(ind.value, color = WallStreetGreen, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
            Text(ind.sentiment.name, color = indColor, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // PATTERN RECOGNITION
    if (state.patterns.isNotEmpty()) {
        Text(">>> PATTERN_DETECTED", color = TextGray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        state.patterns.forEach { pattern ->
            Text(
                text = "[!] ${pattern.pattern.name}: ${pattern.description}",
                color = if (pattern.isBullish) WallStreetGreen else WallStreetRed,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    // FIBONACCI LEVELS
    Text(">>> FIBONACCI_RETRACEMENT", color = TextGray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    state.fibonacci.forEach { (level, price) ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(level, color = TextGray, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            Text("$${String.format(Locale.US, "%.2f", price)}", color = WallStreetAmber, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    OutlinedButton(
        onClick = {
            predictionViewModel.startDeepAnalysis(state.coinId)
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        border = BorderStroke(1.dp, WallStreetGreen),
        shape = RectangleShape,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = WallStreetGreen)
    ) {
        Text(
            text = "> RUN_DEEP_QUANT_SCAN",
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }

    Spacer(modifier = Modifier.height(32.dp))
}