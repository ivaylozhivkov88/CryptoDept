package com.cryptodept.ui.indicators

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.cryptodept.ui.components.TerminalErrorOverlay
import com.cryptodept.ui.components.TerminalLoadingSkeleton
import com.cryptodept.ui.navigation.Screen
import com.cryptodept.ui.theme.*
import com.cryptodept.viewmodel.IndicatorScanResult
import com.cryptodept.viewmodel.IndicatorsUiState
import com.cryptodept.viewmodel.IndicatorsViewModel
import java.util.Locale

@Composable
fun IndicatorsScreen(
    navController: NavController,
    viewModel: IndicatorsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CRTBlack)
            .padding(16.dp)
    ) {
        Text(
            text = ">>> RSI_MACD_SCANNER_v2.1",
            color = WallStreetGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp
        )
        HorizontalDivider(color = GridGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

        when (val state = uiState) {
            is IndicatorsUiState.Loading -> {
                repeat(8) { TerminalLoadingSkeleton(Modifier.padding(vertical = 4.dp)) }
            }
            is IndicatorsUiState.Success -> {
                IndicatorsList(state.results, navController)
            }
            is IndicatorsUiState.Error -> {
                TerminalErrorOverlay(message = state.message, onRetry = { viewModel.scanIndicators() })
            }
        }
    }
}

@Composable
fun IndicatorsList(results: List<IndicatorScanResult>, navController: NavController) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                Text("ASSET", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(1f), fontFamily = FontFamily.Monospace)
                Text("RSI", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(1.2f), fontFamily = FontFamily.Monospace)
                Text("MACD_HIST", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(1.2f), fontFamily = FontFamily.Monospace)
            }
            HorizontalDivider(color = GridGray, thickness = 0.5.dp)
        }
        items(results) { result ->
            IndicatorRow(result) {
                navController.navigate(Screen.Analysis.createRoute(result.coinId))
            }
        }
    }
}

@Composable
fun IndicatorRow(result: IndicatorScanResult, onClick: () -> Unit) {
    val rsiColor = when (result.rsiStatus) {
        "OVERSOLD" -> WallStreetGreen
        "OVERBOUGHT" -> WallStreetRed
        else -> WallStreetAmber
    }
    
    val macdColor = if (result.macdValue > 0) WallStreetGreen else WallStreetRed

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(0.5.dp, GridGray)
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(result.symbol, color = WallStreetAmber, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), fontFamily = FontFamily.Monospace)
        
        Column(modifier = Modifier.weight(1.2f)) {
            Text(String.format(Locale.US, "%.1f", result.rsi), color = rsiColor, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            Text(result.rsiStatus, color = rsiColor.copy(alpha = 0.6f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }

        Column(modifier = Modifier.weight(1.2f)) {
            Text(String.format(Locale.US, "%.4f", result.macdValue), color = macdColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text(result.signal, color = macdColor.copy(alpha = 0.6f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }
    }
}
