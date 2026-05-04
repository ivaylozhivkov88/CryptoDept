package com.cryptodept.ui.tools

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.viewmodel.BacktestUiState
import com.cryptodept.viewmodel.BacktesterViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BacktesterScreen(
    viewModel: BacktesterViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val colors = LocalTerminalColors.current
    val uiState by viewModel.uiState.collectAsState()
    val trackedCoins by viewModel.trackedCoins.collectAsState()
    val selectedCoin by viewModel.selectedCoin.collectAsState()
    
    val rsiEntry by viewModel.rsiEntry.collectAsState()
    val rsiExit by viewModel.rsiExit.collectAsState()
    val stopLoss by viewModel.stopLoss.collectAsState()
    val takeProfit by viewModel.takeProfit.collectAsState()
    val initialCapital by viewModel.initialCapital.collectAsState()
    val riskPerTrade by viewModel.riskPerTrade.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = ">>> STRATEGY BACKTESTER",
            color = colors.primary,
            fontFamily = FontFamily.Monospace,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Input Form
        BacktestForm(
            trackedCoins = trackedCoins,
            selectedCoin = selectedCoin,
            onCoinSelected = { viewModel.selectedCoin.value = it },
            rsiEntry = rsiEntry,
            onRsiEntryChange = { viewModel.rsiEntry.value = it },
            rsiExit = rsiExit,
            onRsiExitChange = { viewModel.rsiExit.value = it },
            stopLoss = stopLoss,
            onStopLossChange = { viewModel.stopLoss.value = it },
            takeProfit = takeProfit,
            onTakeProfitChange = { viewModel.takeProfit.value = it },
            initialCapital = initialCapital,
            onCapitalChange = { viewModel.initialCapital.value = it },
            riskPerTrade = riskPerTrade,
            onRiskChange = { viewModel.riskPerTrade.value = it },
            isLoading = uiState is BacktestUiState.Loading,
            onRun = { viewModel.runBacktest() }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Results Section
        when (val state = uiState) {
            is BacktestUiState.Loading -> {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.primary)
                }
            }
            is BacktestUiState.Success -> {
                BacktestResultsView(state.result)
            }
            is BacktestUiState.Error -> {
                Text("[ERROR] ${state.message}", color = colors.danger, fontFamily = FontFamily.Monospace)
            }
            else -> {}
        }

        Spacer(modifier = Modifier.height(32.dp))
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("< BACK_TO_TOOLS", color = colors.primary, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun BacktestForm(
    trackedCoins: List<String>,
    selectedCoin: String,
    onCoinSelected: (String) -> Unit,
    rsiEntry: Float,
    onRsiEntryChange: (Float) -> Unit,
    rsiExit: Float,
    onRsiExitChange: (Float) -> Unit,
    stopLoss: Float,
    onStopLossChange: (Float) -> Unit,
    takeProfit: Float,
    onTakeProfitChange: (Float) -> Unit,
    initialCapital: Float,
    onCapitalChange: (Float) -> Unit,
    riskPerTrade: Float,
    onRiskChange: (Float) -> Unit,
    isLoading: Boolean,
    onRun: () -> Unit
) {
    val colors = LocalTerminalColors.current
    
    Column(modifier = Modifier.fillMaxWidth().border(1.dp, colors.grid).padding(12.dp)) {
        Text("CONFIG_PARAMETERS", color = colors.dimText, fontSize = 10.sp)
        Spacer(modifier = Modifier.height(8.dp))

        // Coin Selector
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(trackedCoins) { coin ->
                val isSelected = selectedCoin == coin
                Box(
                    modifier = Modifier
                        .border(1.dp, if (isSelected) colors.primary else colors.grid, RectangleShape)
                        .background(if (isSelected) colors.primary.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { onCoinSelected(coin) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(coin.uppercase(), color = if (isSelected) colors.primary else colors.dimText, fontSize = 10.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ParameterSlider(label = "RSI ENTRY THRESHOLD", value = rsiEntry, range = 10f..50f, onValueChange = onRsiEntryChange)
        ParameterSlider(label = "RSI EXIT THRESHOLD", value = rsiExit, range = 50f..90f, onValueChange = onRsiExitChange)
        ParameterSlider(label = "STOP LOSS %", value = stopLoss, range = 1f..20f, onValueChange = onStopLossChange)
        ParameterSlider(label = "TAKE PROFIT %", value = takeProfit, range = 2f..50f, onValueChange = onTakeProfitChange)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onRun,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.background)
        ) {
            Text(if (isLoading) "RUNNING SIMULATION..." else "[RUN BACKTEST]", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ParameterSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    val colors = LocalTerminalColors.current
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = colors.dimText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text(String.format(Locale.US, "%.1f", value), color = colors.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary, inactiveTrackColor = colors.grid)
        )
    }
}

@Composable
fun BacktestResultsView(result: com.cryptodept.domain.usecase.BacktesterEngine.BacktestResult) {
    val colors = LocalTerminalColors.current
    val pnlColor = if (result.totalReturn >= 0) colors.primary else colors.danger
    val sign = if (result.totalReturn >= 0) "+" else ""

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("═══════ SIMULATION RESULTS ═══════", color = colors.grid, modifier = Modifier.align(Alignment.CenterHorizontally))
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Main Return
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$sign${String.format(Locale.US, "%.2f", result.totalReturn)}% ($sign$${String.format(Locale.US, "%,.0f", result.totalReturnUsd)})",
                color = pnlColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
            Text("TOTAL NET RETURN", color = colors.dimText, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Metrics Grid
        Row(modifier = Modifier.fillMaxWidth()) {
            MetricCard(label = "WIN RATE", value = "${String.format(Locale.US, "%.1f", result.winRate)}%", modifier = Modifier.weight(1f))
            MetricCard(label = "TOTAL TRADES", value = result.totalTrades.toString(), modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            MetricCard(label = "MAX DRAWDOWN", value = "-${String.format(Locale.US, "%.1f", result.maxDrawdown)}%", modifier = Modifier.weight(1f))
            MetricCard(label = "PROFIT FACTOR", value = String.format(Locale.US, "%.2f", result.profitFactor), modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Equity Curve
        Text(">>> EQUITY CURVE (PORTFOLIO VALUE)", color = colors.dimText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Box(modifier = Modifier.fillMaxWidth().height(250.dp).background(colors.grid.copy(alpha = 0.1f)).border(1.dp, colors.grid)) {
            EquityChart(result.equityCurve)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Trade List
        Text(">>> TRADE EXECUTION LOG", color = colors.dimText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Column(modifier = Modifier.fillMaxWidth().border(1.dp, colors.grid)) {
            result.trades.takeLast(10).reversed().forEach { trade ->
                TradeRow(trade)
                HorizontalDivider(color = colors.grid, thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = LocalTerminalColors.current
    Column(
        modifier = modifier
            .border(0.5.dp, colors.grid)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = colors.dimText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun EquityChart(data: List<Pair<Long, Double>>) {
    val colors = LocalTerminalColors.current
    AndroidView(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = false
                setTouchEnabled(false)
                setBackgroundColor(AndroidColor.TRANSPARENT)
                
                // PRICHINA 1: Handle empty data
                setNoDataText("AWAITING BACKTEST DATA...")
                setNoDataTextColor(AndroidColor.rgb(0, 255, 65))
                
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    textColor = AndroidColor.LTGRAY
                    setDrawGridLines(true)
                    gridColor = AndroidColor.rgb(0, 59, 0) // grid color
                }
                
                axisLeft.apply {
                    textColor = AndroidColor.LTGRAY
                    setDrawGridLines(true)
                    gridColor = AndroidColor.rgb(0, 59, 0)
                }
                axisRight.isEnabled = false
            }
        },
        update = { chart ->
            if (data.isNullOrEmpty()) {
                chart.clear()
                chart.invalidate()
                return@AndroidView
            }

            val entries = data.mapIndexed { index, pair ->
                Entry(index.toFloat(), pair.second.toFloat())
            }
            val dataSet = LineDataSet(entries, "Equity").apply {
                color = AndroidColor.rgb(0, 255, 65)
                setDrawCircles(false)
                setDrawValues(false)
                lineWidth = 2f
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }
            chart.data = LineData(dataSet)
            chart.invalidate()
        }
    )
}

@Composable
fun TradeRow(trade: com.cryptodept.domain.usecase.BacktesterEngine.SimulatedTrade) {
    val colors = LocalTerminalColors.current
    val pnlColor = if (trade.pnlUsd >= 0) colors.primary else colors.danger
    val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.US)
    
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(sdf.format(Date(trade.entryTimestamp)), color = colors.dimText, fontSize = 9.sp)
            Text("$${String.format(Locale.US, "%,.2f", trade.entryPrice)} → $${String.format(Locale.US, "%,.2f", trade.exitPrice)}", color = colors.textPrimary, fontSize = 11.sp)
        }
        Text(
            text = "${if (trade.pnlUsd >= 0) "+" else ""}${String.format(Locale.US, "%.1f", trade.pnlPercent)}%",
            color = pnlColor,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
