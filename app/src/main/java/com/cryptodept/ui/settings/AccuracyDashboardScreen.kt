package com.cryptodept.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.viewmodel.AccuracyDashboardViewModel
import java.util.Locale

@Composable
fun AccuracyDashboardScreen(
    onBack: () -> Unit,
    viewModel: AccuracyDashboardViewModel = hiltViewModel()
) {
    val colors = LocalTerminalColors.current
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                ">>> PREDICTION_TRACK_RECORD",
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onBack) {
                Text("[X]", color = colors.danger, fontFamily = FontFamily.Monospace)
            }
        }

        HorizontalDivider(color = colors.grid, modifier = Modifier.padding(vertical = 12.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                Text("HISTORICAL PERFORMANCE SUMMARY", color = colors.amber, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().border(1.dp, colors.grid).padding(16.dp)) {
                    Column {
                        if (state.totalSamples == 0) {
                            Text("CALCULATING...", color = colors.amber, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text("Awaiting first verified market close", color = colors.dimText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        } else {
                            Text("OVERALL ACCURACY: ${String.format(Locale.US, "%.1f", state.overallAccuracy)}%", color = colors.primary, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text("Based on ${state.totalSamples} verified predictions", color = colors.dimText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            item {
                Text("PER_MODEL_STATS", color = colors.amber, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(8.dp))
            }

            items(state.modelStats) { stat ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stat.name, color = colors.textPrimary, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    Text("${stat.accuracy}%", color = if (stat.accuracy > 50) colors.primary else colors.danger, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
                Text("MARKET_REGIME_PERFORMANCE", color = colors.amber, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(8.dp))
            }

            items(state.regimeStats) { stat ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stat.name, color = colors.textPrimary, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    Text("${stat.accuracy}%", color = if (stat.accuracy > 50) colors.primary else colors.danger, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                }
            }
        }
    }
}
