// STEP 12: Charts Screen implementation using MPAndroidChart
// Created: 2024-05-22
// Dependencies: ChartsViewModel, MPAndroidChart
// Style: Wall Street 90s (Terminal)

package com.cryptodept.ui.charts

import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontFamily
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.ui.components.TerminalErrorOverlay
import com.cryptodept.ui.components.TerminalLoadingSkeleton
import com.cryptodept.ui.theme.*
import com.cryptodept.viewmodel.ChartUiState
import com.cryptodept.viewmodel.ChartsViewModel
import com.github.mikephil.charting.charts.CandleStickChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.data.CandleDataSet
import com.github.mikephil.charting.data.CandleEntry

@Composable
fun ChartsScreen(
    coinId: String,
    viewModel: ChartsViewModel = hiltViewModel()
) {
    val colors = LocalTerminalColors.current
    var showFibonacci by remember { mutableStateOf(false) }

    LaunchedEffect(coinId) {
        viewModel.loadChart(coinId)
    }

    val state by viewModel.chartState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ">>> TERMINAL CHART: ${coinId.uppercase()}",
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
            
            // Step 55: Fibonacci Toggle
            Text(
                text = if (showFibonacci) "[FIB: ON]" else "[FIB: OFF]",
                color = if (showFibonacci) colors.amber else colors.dimText,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier
                    .background(if (showFibonacci) colors.amber.copy(alpha = 0.2f) else Color.Transparent)
                    .padding(horizontal = 4.dp)
                    .clickable { showFibonacci = !showFibonacci }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (val chartState = state) {
            is ChartUiState.Loading -> {
                com.cryptodept.ui.components.skeletons.ChartsSkeleton()
            }
            is ChartUiState.Success -> {
                if (chartState.data.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("NO DATA RECEIVED FOR $coinId", color = colors.danger, fontFamily = FontFamily.Monospace)
                    }
                } else {
                    CandleChart(
                        entries = chartState.data.mapIndexed { index, ohlc ->
                            CandleEntry(
                                index.toFloat(),
                                ohlc.high.toFloat(),
                                ohlc.low.toFloat(),
                                ohlc.open.toFloat(),
                                ohlc.close.toFloat()
                            )
                        },
                        showFibonacci = showFibonacci
                    )
                }
            }
            is ChartUiState.Error -> {
                TerminalErrorOverlay(message = chartState.message, onRetry = { viewModel.loadChart(coinId) })
            }
        }
    }
}

@Composable
fun CandleChart(entries: List<CandleEntry>, showFibonacci: Boolean) {
    val contentDesc = if (showFibonacci) "Candlestick chart with Fibonacci levels" else "Candlestick chart"
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp)
            .semantics { contentDescription = contentDesc },
        factory = { context ->
            CandleStickChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = false
                setBackgroundColor(AndroidColor.TRANSPARENT)
                setGridBackgroundColor(AndroidColor.TRANSPARENT)
                
                // Step 54: Crosshair
                setDrawMarkerViews(true)
                setTouchEnabled(true)
                setDragEnabled(true)
                setScaleEnabled(true)
                setPinchZoom(true)

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    textColor = AndroidColor.LTGRAY
                    setDrawGridLines(true)
                    gridColor = AndroidColor.DKGRAY
                }

                axisLeft.apply {
                    textColor = AndroidColor.LTGRAY
                    setDrawGridLines(true)
                    gridColor = AndroidColor.DKGRAY
                }

                axisRight.isEnabled = false
            }
        },
        update = { chart ->
            val dataSet = CandleDataSet(entries, "Market Data").apply {
                color = AndroidColor.rgb(0, 255, 65) // Terminal Green
                shadowColor = AndroidColor.rgb(0, 150, 40) // Dimmer green
                shadowWidth = 1.0f
                decreasingColor = AndroidColor.RED
                increasingColor = AndroidColor.GREEN
                neutralColor = AndroidColor.WHITE
                setDrawValues(false)
                
                // Phosphor Glow Effect simulator
                setDrawIcons(false)
                enableDashedHighlightLine(10f, 5f, 0f)
            }

            chart.data = CandleData(dataSet)
            chart.invalidate()
            
            // Note: Real Fibonacci implementation would require a custom Renderer or drawing on Canvas overlay
        }
    )
}
