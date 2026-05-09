package com.cryptodept.ui.portfolio

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.domain.model.*
import com.cryptodept.ui.theme.*
import com.cryptodept.viewmodel.PortfolioUiState
import com.cryptodept.viewmodel.PortfolioViewModel
import java.util.*

@Composable
fun PortfolioScreen(
    navController: androidx.navigation.NavController,
    viewModel: PortfolioViewModel = hiltViewModel(),
) {
    val colors = LocalTerminalColors.current
    val uiState by viewModel.uiState.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }

    if (showHelp) {
        com.cryptodept.ui.components
            .TerminalHelpDialog(onDismiss = { showHelp = false })
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(16.dp),
    ) {
        Text(
            text = ">>> PORTFOLIO TERMINAL",
            color = colors.primary,
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(16.dp))

        when (val state = uiState) {
            is PortfolioUiState.Loading -> {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("SYNCING HOLDINGS...", color = colors.primary, fontFamily = FontFamily.Monospace)
                }
            }
            is PortfolioUiState.Success -> {
                PortfolioContent(
                    summary = state.summary,
                    onAddClick = { showAddDialog = true },
                )
            }
            is PortfolioUiState.Error -> {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("[ERROR] ${state.message}", color = colors.danger, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        com.cryptodept.ui.components.TerminalCommandBar(
            onCommandEntered = { cmd ->
                val parts = cmd.uppercase().split(" ")
                when (parts[0]) {
                    "HELP" -> showHelp = true
                    "LOGOUT" -> viewModel.setAdminStatus(false)
                    else ->
                        com.cryptodept.ui.analysis
                            .handleGlobalCommand(cmd, navController)
                }
            },
        )
    }

    if (showAddDialog) {
        AddPositionDialog(
            viewModel = viewModel,
            onDismiss = { showAddDialog = false },
        )
    }
}

@Composable
fun PortfolioContent(
    summary: PortfolioSummary,
    onAddClick: () -> Unit,
) {
    val colors = LocalTerminalColors.current

    Column(modifier = Modifier.fillMaxSize()) {
        // SUMMARY BOX
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.grid, RectangleShape)
                    .padding(16.dp),
        ) {
            Column {
                SummaryRow("TOTAL VALUE:", "$${String.format(Locale.US, "%,.2f", summary.totalValueUsd)}", colors.primary)
                SummaryRow("COST BASIS:", "$${String.format(Locale.US, "%,.2f", summary.totalCostUsd)}", colors.textPrimary)

                val pnlColor = if (summary.totalPnlUsd >= 0) colors.primary else colors.danger
                val sign = if (summary.totalPnlUsd >= 0) "+" else ""
                SummaryRow(
                    label = "TOTAL P&L:",
                    value = "$sign$${String.format(
                        Locale.US,
                        "%,.2f",
                        summary.totalPnlUsd,
                    )} ($sign${String.format(Locale.US, "%.2f", summary.totalPnlPercent)}%)",
                    valueColor = pnlColor,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ALLOCATION BAR
        AllocationBar(summary.entries)

        Spacer(modifier = Modifier.height(16.dp))

        // HOLDINGS LIST
        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                Text(
                    ">>> HOLDINGS",
                    color = colors.dimText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            items(summary.entries) { item ->
                HoldingRow(item)
                HorizontalDivider(color = colors.grid, thickness = 0.5.dp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onAddClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.background),
        ) {
            Text("[+ ADD POSITION]", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SummaryRow(
    label: String,
    value: String,
    valueColor: Color,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = LocalTerminalColors.current.dimText, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = valueColor, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AllocationBar(entries: List<PortfolioEntryWithCurrentPrice>) {
    val colors = LocalTerminalColors.current
    val totalValue = entries.sumOf { it.currentValueUsd }
    if (totalValue <= 0) return

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(12.dp)
                .border(1.dp, colors.grid, RectangleShape),
    ) {
        entries.forEachIndexed { index, item ->
            val weight = (item.currentValueUsd / totalValue).toFloat()
            val brightness = 0.3f + (index % 5) * 0.15f
            Box(
                modifier =
                    Modifier
                        .weight(weight.coerceAtLeast(0.01f))
                        .fillMaxHeight()
                        .background(colors.primary.copy(alpha = brightness)),
            )
        }
    }
}

@Composable
fun HoldingRow(item: PortfolioEntryWithCurrentPrice) {
    val colors = LocalTerminalColors.current
    val pnlColor = if (item.pnlUsd >= 0) colors.primary else colors.danger
    val sign = if (item.pnlUsd >= 0) "+" else ""

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "${item.entry.symbol}  ${String.format(
                    Locale.US,
                    "%.4f",
                    item.entry.quantity,
                )} @ $${String.format(Locale.US, "%,.2f", item.entry.averageEntryPrice)} avg",
                color = colors.textPrimary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                text = "NOW: $${String.format(Locale.US, "%,.2f", item.currentPrice)}",
                color = colors.amber,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Box(
                modifier =
                    Modifier
                        .height(2.dp)
                        .width(40.dp)
                        .background(pnlColor)
                        .align(Alignment.CenterVertically),
            )
            Text(
                text = "P&L: $sign$${String.format(
                    Locale.US,
                    "%,.2f",
                    item.pnlUsd,
                )} ($sign${String.format(Locale.US, "%.2f", item.pnlPercent)}%)",
                color = pnlColor,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun AddPositionDialog(
    viewModel: PortfolioViewModel,
    onDismiss: () -> Unit,
) {
    val colors = LocalTerminalColors.current
    val trackedCoins by viewModel.trackedCoins.collectAsState()

    var selectedCoinIdx by remember { mutableIntStateOf(0) }
    var quantity by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.background,
        modifier = Modifier.border(1.dp, colors.primary, RectangleShape),
        title = { Text("ADD POSITION", color = colors.primary, fontFamily = FontFamily.Monospace) },
        text = {
            Column {
                if (trackedCoins.isNotEmpty()) {
                    Text("COIN SELECTOR:", color = colors.dimText, fontSize = 10.sp)
                    ScrollableTabRow(
                        selectedTabIndex = selectedCoinIdx,
                        containerColor = colors.background,
                        contentColor = colors.primary,
                        edgePadding = 0.dp,
                        divider = {},
                    ) {
                        trackedCoins.forEachIndexed { index, (id, symbol) ->
                            Tab(
                                selected = selectedCoinIdx == index,
                                onClick = { selectedCoinIdx = index },
                                text = { Text(symbol, fontSize = 10.sp) },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("QUANTITY", color = colors.dimText) },
                    textStyle = TextStyle(color = colors.primary, fontFamily = FontFamily.Monospace),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.grid,
                        ),
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("AVG ENTRY PRICE", color = colors.dimText) },
                    textStyle = TextStyle(color = colors.primary, fontFamily = FontFamily.Monospace),
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.grid,
                        ),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val q = quantity.toDoubleOrNull() ?: 0.0
                val p = price.toDoubleOrNull() ?: 0.0
                if (q > 0 && p > 0 && trackedCoins.isNotEmpty()) {
                    val coin = trackedCoins[selectedCoinIdx]
                    viewModel.addPosition(coin.first, coin.second, q, p)
                    onDismiss()
                }
            }) {
                Text("CONFIRM", color = colors.primary, fontFamily = FontFamily.Monospace)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = colors.dimText, fontFamily = FontFamily.Monospace)
            }
        },
    )
}
