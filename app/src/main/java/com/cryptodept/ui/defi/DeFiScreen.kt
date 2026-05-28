package com.cryptodept.ui.defi

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.domain.model.DeFiProtocol
import com.cryptodept.domain.model.DeFiYieldOpportunity
import com.cryptodept.domain.model.LpSimulationResult
import com.cryptodept.ui.components.TerminalErrorOverlay
import com.cryptodept.ui.components.TerminalLoadingSkeleton
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.util.TerminalConfig
import com.cryptodept.viewmodel.DeFiUiState
import com.cryptodept.viewmodel.DeFiViewModel
import java.util.Locale

@Composable
fun DeFiScreen(
    onBack: () -> Unit,
    viewModel: DeFiViewModel = hiltViewModel(),
) {
    val colors = LocalTerminalColors.current
    val uiState by viewModel.uiState.collectAsState()
    val simResult by viewModel.simulationResult.collectAsState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(TerminalConfig.UI.DEFAULT_PADDING),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = ">>> DEFI_INTELLIGENCE_HUB",
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = TerminalConfig.UI.FONT_SIZE_HEADER,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onBack) {
                Text("[X]", color = colors.danger, fontFamily = FontFamily.Monospace)
            }
        }

        HorizontalDivider(color = colors.grid, modifier = Modifier.padding(vertical = TerminalConfig.UI.SPACER_MEDIUM))

        when (val state = uiState) {
            is DeFiUiState.Loading -> {
                TerminalLoadingSkeleton(Modifier.fillMaxSize())
            }
            is DeFiUiState.Success -> {
                DeFiContent(
                    protocols = state.protocols,
                    yields = state.yields,
                    simResult = simResult,
                    onSimulate = { inv, pA, pB, apy, days ->
                        viewModel.runSimulation(inv, pA, pB, apy, days)
                    }
                )
            }
            is DeFiUiState.Error -> {
                TerminalErrorOverlay(message = state.message, onRetry = { viewModel.loadData() })
            }
        }
    }
}

@Composable
fun DeFiContent(
    protocols: List<DeFiProtocol>,
    yields: List<DeFiYieldOpportunity>,
    simResult: LpSimulationResult?,
    onSimulate: (Double, Double, Double, Double, Int) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val colors = LocalTerminalColors.current

    Column {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = colors.primary,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = colors.primary,
                )
            },
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("PROTOCOLS", modifier = Modifier.padding(8.dp), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("YIELDS", modifier = Modifier.padding(8.dp), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                Text("LP_SIMULATOR", modifier = Modifier.padding(8.dp), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> ProtocolsList(protocols)
            1 -> YieldsList(yields)
            2 -> LpSimulator(simResult, onSimulate)
        }
    }
}

@Composable
fun ProtocolsList(protocols: List<DeFiProtocol>) {
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(protocols) { protocol ->
            ProtocolRow(protocol)
        }
    }
}

@Composable
fun YieldsList(yields: List<DeFiYieldOpportunity>) {
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(yields) { yield ->
            YieldRow(yield)
        }
    }
}

@Composable
fun LpSimulator(
    result: LpSimulationResult?,
    onSimulate: (Double, Double, Double, Double, Int) -> Unit
) {
    val colors = LocalTerminalColors.current
    var initialInvestment by remember { mutableStateOf("1000") }
    var priceChangeA by remember { mutableStateOf("1.0") }
    var priceChangeB by remember { mutableStateOf("1.5") }
    var apy by remember { mutableStateOf("10.0") }
    var days by remember { mutableStateOf("30") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(8.dp)
    ) {
        Text("--- LP IMPERMANENT LOSS SIMULATOR ---", color = colors.dimText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(12.dp))

        SimField("INITIAL_INVESTMENT ($)", initialInvestment) { initialInvestment = it }
        SimField("PRICE_CHANGE_ASSET_A (ratio)", priceChangeA) { priceChangeA = it }
        SimField("PRICE_CHANGE_ASSET_B (ratio)", priceChangeB) { priceChangeB = it }
        SimField("ANNUAL_YIELD (%)", apy) { apy = it }
        SimField("DURATION (DAYS)", days) { days = it }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                onSimulate(
                    initialInvestment.toDoubleOrNull() ?: 1000.0,
                    priceChangeA.toDoubleOrNull() ?: 1.0,
                    priceChangeB.toDoubleOrNull() ?: 1.0,
                    apy.toDoubleOrNull() ?: 0.0,
                    days.toIntOrNull() ?: 30
                )
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
            shape = RectangleShape
        ) {
            Text("RUN_SIMULATION", color = colors.background, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }

        result?.let {
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.primary)
                    .padding(16.dp)
            ) {
                Column {
                    Text(">>> SIMULATION_RESULTS", color = colors.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    ResultRow("POOL_VALUE_AT_EXIT", "$${String.format(Locale.US, "%.2f", it.finalValue)}")
                    ResultRow("IMPERMANENT_LOSS", "${String.format(Locale.US, "%.2f", it.impermanentLoss)}%", color = colors.danger)
                    ResultRow("YIELD_ACCUMULATED", "$${String.format(Locale.US, "%.2f", it.gainWithYield)}", color = colors.primary)
                    HorizontalDivider(color = colors.grid, modifier = Modifier.padding(vertical = 8.dp))
                    ResultRow("NET_PROFIT/LOSS", "$${String.format(Locale.US, "%.2f", it.netProfit)}", 
                        color = if (it.netProfit >= 0) colors.primary else colors.danger,
                        bold = true
                    )
                }
            }
        }
    }
}

@Composable
fun SimField(label: String, value: String, onValueChange: (String) -> Unit) {
    val colors = LocalTerminalColors.current
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(label, color = colors.dimText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = androidx.compose.ui.text.TextStyle(color = colors.primary, fontFamily = FontFamily.Monospace, fontSize = 14.sp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.grid,
                cursorColor = colors.primary
            ),
            singleLine = true
        )
    }
}

@Composable
fun ResultRow(label: String, value: String, color: Color = LocalTerminalColors.current.textPrimary, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = LocalTerminalColors.current.dimText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = color, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun ProtocolRow(protocol: DeFiProtocol) {
    val colors = LocalTerminalColors.current
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(0.5.dp, colors.grid)
                .padding(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    protocol.name.uppercase(),
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                )
                Text(protocol.category.uppercase(), color = colors.dimText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "TVL: $${formatTvl(protocol.tvl)}",
                    color = colors.amber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                )
                val changeColor = if (protocol.tvlChange1d >= 0) colors.primary else colors.danger
                Text(
                    text = "24H: ${if (protocol.tvlChange1d >= 0) "+" else ""}${String.format(Locale.US, "%.2f", protocol.tvlChange1d)}%",
                    color = changeColor,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

@Composable
fun YieldRow(yield: DeFiYieldOpportunity) {
    val colors = LocalTerminalColors.current
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(0.5.dp, colors.grid)
                .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${yield.symbol} on ${yield.protocol.uppercase()}",
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                )
                Text("CHAIN: ${yield.chain}", color = colors.dimText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${String.format(Locale.US, "%.2f", yield.apy)}% APY",
                    color = colors.primary,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    text = "TVL: $${formatTvl(yield.tvl)}",
                    color = colors.dimText,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

fun formatTvl(tvl: Double): String =
    when {
        tvl >= 1_000_000_000 -> String.format(Locale.US, "%.2fB", tvl / 1_000_000_000)
        tvl >= 1_000_000 -> String.format(Locale.US, "%.2fM", tvl / 1_000_000)
        else -> String.format(Locale.US, "%.0f", tvl)
    }
