package com.cryptodept.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.viewmodel.JournalViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeJournalScreen(viewModel: JournalViewModel = hiltViewModel()) {
    val colors = LocalTerminalColors.current
    val uiState by viewModel.uiState.collectAsState()
    val trades by viewModel.allTrades.collectAsState()
    val stats by viewModel.journalStats.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = colors.primary,
                contentColor = colors.background,
                shape = RectangleShape,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Trade")
            }
        },
        containerColor = colors.background,
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .background(colors.background),
        ) {
            when (val state = uiState) {
                is JournalViewModel.JournalUiState.Loading -> {
                    com.cryptodept.ui.components.skeletons
                        .JournalSkeleton()
                }
                is JournalViewModel.JournalUiState.Error -> {
                    Text(
                        text = "[ERROR] ${state.message}", 
                        color = colors.danger, 
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is JournalViewModel.JournalUiState.Success -> {
                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .border(1.dp, colors.grid, RectangleShape),
                    ) {
                        item {
                            HeaderSection()
                            StatsSection(stats)
                        }

                        item {
                            SectionDivider("OPEN POSITIONS")
                        }
                        val openTrades = trades.filter { it.status == TradeStatus.OPEN }
                        if (openTrades.isEmpty()) {
                            item {
                                Text(
                                    ">>> NO OPEN POSITIONS",
                                    color = colors.dimText,
                                    modifier = Modifier.padding(16.dp),
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                        } else {
                            items(openTrades) { trade ->
                                TradeItem(trade) { exitPrice -> viewModel.closeTrade(trade, exitPrice) }
                            }
                        }

                        item {
                            SectionDivider("CLOSED TRADES")
                        }
                        val closedTrades = trades.filter { it.status != TradeStatus.OPEN }
                        items(closedTrades) { trade ->
                            TradeItem(trade) {}
                        }
                    }
                }
            }
        }

        if (showAddSheet) {
            AddTradeBottomSheet(
                onDismiss = { showAddSheet = false },
                onSave = { viewModel.addTrade(it) },
            )
        }
    }
}

@Composable
fun HeaderSection() {
    val colors = LocalTerminalColors.current
    Box(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Text(
            text = ">>> TRADE JOURNAL",
            color = colors.primary,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
    }
    HorizontalDivider(color = colors.grid, thickness = 1.dp)
}

@Composable
fun StatsSection(stats: JournalStats) {
    val colors = LocalTerminalColors.current
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "═ STATISTICS " + "═".repeat(20),
            color = colors.primary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatRow("WIN RATE:", "${String.format(Locale.US, "%.1f", stats.winRate)}%")
            StatRow("TOTAL TRADES:", stats.totalTrades.toString())
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            StatRow("AVG R:R:", "${String.format(Locale.US, "%.1f", stats.averageRR)}:1")
        }
        StatRow(
            "TOTAL P&L:",
            "${if (stats.averagePnL >= 0) "+" else ""}$${String.format(Locale.US, "%,.0f", stats.averagePnL)}",
            if (stats.averagePnL >= 0) colors.primary else colors.error,
        )
    }
    HorizontalDivider(color = colors.grid, thickness = 1.dp)
}

@Composable
fun StatRow(
    label: String,
    value: String,
    valueColor: Color? = null,
) {
    val colors = LocalTerminalColors.current
    val resolvedColor = valueColor ?: colors.textPrimary
    Row {
        Text(text = label.padEnd(14), color = colors.dimText, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Text(text = value, color = resolvedColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun SectionDivider(title: String) {
    val colors = LocalTerminalColors.current
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(vertical = 6.dp, horizontal = 12.dp),
    ) {
        Text(
            text = "═ $title " + "═".repeat(15),
            color = colors.primary,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
        )
    }
}

@Composable
fun TradeItem(
    trade: TradeJournal,
    onClose: (Double) -> Unit,
) {
    val colors = LocalTerminalColors.current
    val dateStr = SimpleDateFormat("MMM dd", Locale.US).format(Date(trade.entryTime))
    val isWin = trade.status == TradeStatus.CLOSED_WIN
    val isLoss = trade.status == TradeStatus.CLOSED_LOSS
    val accentColor =
        if (trade.status == TradeStatus.OPEN) {
            colors.textPrimary
        } else if (isWin) {
            colors.primary
        } else if (isLoss) {
            colors.error
        } else {
            colors.dimText
        }

    Column(modifier = Modifier.padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "${if (trade.direction == TradeDirection.LONG) "▲" else "▼"} ${trade.direction} ${trade.symbol}",
                color = accentColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
            Text(text = dateStr, color = colors.dimText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
        Text(
            text =
                "Entry: $${String.format(Locale.US, "%,.2f", trade.entryPrice)}" +
                    if (trade.exitPrice != null) " Exit: $${String.format(Locale.US, "%,.2f", trade.exitPrice)}" else "",
            color = colors.dimText,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
        )
        if (trade.pnlPercent != null) {
            Text(
                text = "P&L: ${if (trade.pnlPercent >= 0) "+" else ""}${String.format(
                    Locale.US,
                    "%.2f",
                    trade.pnlPercent,
                )}% ($${String.format(Locale.US, "%,.2f", trade.pnlUsd)})",
                color = if (trade.pnlPercent >= 0) colors.primary else colors.error,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
            )
        }

        if (trade.status == TradeStatus.OPEN) {
            var exitPriceInput by remember { mutableStateOf("") }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                TextField(
                    value = exitPriceInput,
                    onValueChange = { exitPriceInput = it },
                    placeholder = { Text("Exit Price", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors =
                        TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                        ),
                    singleLine = true,
                )
                Button(
                    onClick = { exitPriceInput.toDoubleOrNull()?.let { onClose(it) } },
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = colors.primary),
                    modifier = Modifier.border(1.dp, colors.primary, RectangleShape),
                ) {
                    Text("[CLOSE]", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
    HorizontalDivider(color = colors.grid.copy(alpha = 0.3f), thickness = 0.5.dp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTradeBottomSheet(
    onDismiss: () -> Unit,
    onSave: (TradeJournal) -> Unit,
) {
    val colors = LocalTerminalColors.current
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.surface, shape = RectangleShape) {
        var symbol by remember { mutableStateOf("BTC") }
        var direction by remember { mutableStateOf(TradeDirection.LONG) }
        var entryPrice by remember { mutableStateOf("") }
        var quantity by remember { mutableStateOf("") }

        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text(
                ">>> NEW TRADE",
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = symbol,
                onValueChange = { symbol = it },
                label = { Text("Symbol", fontFamily = FontFamily.Monospace) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace)
            )
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton(onClick = {
                    direction = TradeDirection.LONG
                }, modifier = Modifier.border(if (direction == TradeDirection.LONG) 1.dp else 0.dp, colors.primary)) {
                    Text("LONG", color = if (direction == TradeDirection.LONG) colors.primary else colors.dimText, fontFamily = FontFamily.Monospace)
                }
                TextButton(onClick = {
                    direction = TradeDirection.SHORT
                }, modifier = Modifier.border(if (direction == TradeDirection.SHORT) 1.dp else 0.dp, colors.primary)) {
                    Text("SHORT", color = if (direction == TradeDirection.SHORT) colors.primary else colors.dimText, fontFamily = FontFamily.Monospace)
                }
            }
            OutlinedTextField(
                value = entryPrice, 
                onValueChange = { entryPrice = it }, 
                label = { Text("Entry Price", fontFamily = FontFamily.Monospace) }, 
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace)
            )
            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it },
                label = { Text("Quantity", fontFamily = FontFamily.Monospace) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace)
            )

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val entry = entryPrice.toDoubleOrNull() ?: 0.0
                    val qty = quantity.toDoubleOrNull() ?: 0.0
                    onSave(
                        TradeJournal(
                            id = UUID.randomUUID().toString(),
                            coinId = symbol.lowercase(),
                            symbol = symbol,
                            direction = direction,
                            entryPrice = entry,
                            exitPrice = null,
                            quantity = qty,
                            entryTime = System.currentTimeMillis(),
                            exitTime = null,
                            riskPercent = 0.0,
                            stopLoss = null,
                            takeProfit = null,
                            notes = "",
                            status = TradeStatus.OPEN,
                            pnlUsd = null,
                            pnlPercent = null,
                            riskRewardActual = null,
                            positionSizeUsd = entry * qty,
                            marketConditions = "",
                        ),
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.background),
            ) {
                Text("[SAVE TRADE]", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
