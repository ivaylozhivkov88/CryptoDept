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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.data.db.TradeJournalEntity
import com.cryptodept.viewmodel.JournalStats
import com.cryptodept.viewmodel.JournalViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeJournalScreen(
    viewModel: JournalViewModel = hiltViewModel()
) {
    val trades by viewModel.allTrades.collectAsState()
    val stats by viewModel.stats.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = Color(0xFF00FF41),
                contentColor = Color.Black,
                shape = RectangleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Trade")
            }
        },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .background(Color.Black)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, Color(0xFF00FF41), RectangleShape)
            ) {
                item {
                    HeaderSection()
                    StatsSection(stats)
                }

                item {
                    SectionDivider("OPEN POSITIONS")
                }
                val openTrades = trades.filter { it.status == "OPEN" }
                if (openTrades.isEmpty()) {
                    item { Text(">>> NO OPEN POSITIONS", color = Color.Gray, modifier = Modifier.padding(16.dp), fontFamily = com.cryptodept.ui.theme.JetBrainsMono) }
                } else {
                    items(openTrades) { trade ->
                        TradeItem(trade) { exitPrice -> viewModel.closeTrade(trade, exitPrice) }
                    }
                }

                item {
                    SectionDivider("CLOSED TRADES")
                }
                val closedTrades = trades.filter { it.status != "OPEN" }
                items(closedTrades) { trade ->
                    TradeItem(trade) {}
                }
            }
        }
        
        if (showAddSheet) {
            AddTradeBottomSheet(
                onDismiss = { showAddSheet = false },
                onSave = { viewModel.addTrade(it) }
            )
        }
    }
}

@Composable
fun HeaderSection() {
    Box(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Text(
            text = ">>> TRADE JOURNAL",
            color = Color(0xFF00FF41),
            fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
    HorizontalDivider(color = Color(0xFF00FF41), thickness = 1.dp)
}

@Composable
fun StatsSection(stats: JournalStats) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("═ STATISTICS " + "═".repeat(30), color = Color(0xFF00FF41), fontSize = 12.sp, fontFamily = com.cryptodept.ui.theme.JetBrainsMono)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            StatRow("WIN RATE:", "${String.format(Locale.US, "%.1f", stats.winRate * 100)}%")
            StatRow("TOTAL TRADES:", stats.totalTrades.toString())
        }
        Row(modifier = Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            StatRow("AVG R:R:", "${String.format(Locale.US, "%.1f", stats.avgRR)}:1")
            StatRow("OPEN:", stats.openTrades.toString())
        }
        StatRow("TOTAL P&L:", "${if (stats.totalPnLUsd >= 0) "+" else ""}$${String.format(Locale.US, "%,.0f", stats.totalPnLUsd)}", if (stats.totalPnLUsd >= 0) Color(0xFF00FF41) else Color(0xFFFF3B30))
    }
    HorizontalDivider(color = Color(0xFF00FF41), thickness = 1.dp)
}

@Composable
fun StatRow(label: String, value: String, valueColor: Color = Color.White) {
    Row {
        Text(text = label.padEnd(14), color = Color.Gray, fontSize = 12.sp, fontFamily = com.cryptodept.ui.theme.JetBrainsMono)
        Text(text = value, color = valueColor, fontSize = 12.sp, fontFamily = com.cryptodept.ui.theme.JetBrainsMono)
    }
}

@Composable
fun SectionDivider(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF111111))
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Text(
            text = "═ $title " + "═".repeat(20),
            color = Color(0xFF00FF41),
            fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
            fontSize = 12.sp
        )
    }
}

@Composable
fun TradeItem(trade: TradeJournalEntity, onClose: (Double) -> Unit) {
    val dateStr = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(trade.entryTime))
    val isWin = trade.status == "CLOSED_WIN"
    val isLoss = trade.status == "CLOSED_LOSS"
    val accentColor = if (trade.status == "OPEN") Color.White else if (isWin) Color(0xFF00FF41) else if (isLoss) Color(0xFFFF3B30) else Color.Gray

    Column(modifier = Modifier.padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(
                text = "${if (trade.direction == "LONG") "▲" else "▼"} ${trade.direction} ${trade.symbol}",
                color = accentColor,
                fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                fontWeight = FontWeight.Bold
            )
            Text(text = dateStr, color = Color.Gray, fontSize = 10.sp, fontFamily = com.cryptodept.ui.theme.JetBrainsMono)
        }
        Text(
            text = "Entry: $${String.format(Locale.US, "%,.2f", trade.entryPrice)}" + if (trade.exitPrice != null) " Exit: $${String.format(Locale.US, "%,.2f", trade.exitPrice)}" else "",
            color = Color.Gray,
            fontSize = 12.sp,
            fontFamily = com.cryptodept.ui.theme.JetBrainsMono
        )
        if (trade.pnlPercent != null) {
            Text(
                text = "P&L: ${if (trade.pnlPercent!! >= 0) "+" else ""}${String.format(Locale.US, "%.2f", trade.pnlPercent)}% ($${String.format(Locale.US, "%,.2f", trade.pnlUsd)})",
                color = if (trade.pnlPercent!! >= 0) Color(0xFF00FF41) else Color(0xFFFF3B30),
                fontSize = 12.sp,
                fontFamily = com.cryptodept.ui.theme.JetBrainsMono
            )
        }
        
        if (trade.status == "OPEN") {
            var exitPriceInput by remember { mutableStateOf("") }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                TextField(
                    value = exitPriceInput,
                    onValueChange = { exitPriceInput = it },
                    placeholder = { Text("Exit Price", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )
                Button(
                    onClick = { exitPriceInput.toDoubleOrNull()?.let { onClose(it) } },
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color(0xFF00FF41)),
                    modifier = Modifier.border(1.dp, Color(0xFF00FF41), RectangleShape)
                ) {
                    Text("[CLOSE]", fontSize = 10.sp, fontFamily = com.cryptodept.ui.theme.JetBrainsMono)
                }
            }
        }
    }
    HorizontalDivider(color = Color(0xFF00FF41).copy(alpha = 0.2f), thickness = 0.5.dp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTradeBottomSheet(onDismiss: () -> Unit, onSave: (TradeJournalEntity) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF111111), shape = RectangleShape) {
        var symbol by remember { mutableStateOf("BTC") }
        var direction by remember { mutableStateOf("LONG") }
        var entryPrice by remember { mutableStateOf("") }
        var quantity by remember { mutableStateOf("") }
        
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text(">>> NEW TRADE", color = Color(0xFF00FF41), fontFamily = com.cryptodept.ui.theme.JetBrainsMono, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            // Simple inputs
            OutlinedTextField(value = symbol, onValueChange = { symbol = it }, label = { Text("Symbol") }, modifier = Modifier.fillMaxWidth())
            Row(modifier = Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                TextButton(onClick = { direction = "LONG" }, modifier = Modifier.border(if (direction == "LONG") 1.dp else 0.dp, Color(0xFF00FF41))) { Text("LONG", color = if (direction == "LONG") Color(0xFF00FF41) else Color.Gray) }
                TextButton(onClick = { direction = "SHORT" }, modifier = Modifier.border(if (direction == "SHORT") 1.dp else 0.dp, Color(0xFF00FF41))) { Text("SHORT", color = if (direction == "SHORT") Color(0xFF00FF41) else Color.Gray) }
            }
            OutlinedTextField(value = entryPrice, onValueChange = { entryPrice = it }, label = { Text("Entry Price") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Quantity") }, modifier = Modifier.fillMaxWidth())
            
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val entry = entryPrice.toDoubleOrNull() ?: 0.0
                    val qty = quantity.toDoubleOrNull() ?: 0.0
                    onSave(TradeJournalEntity(
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
                        status = "OPEN",
                        pnlUsd = null,
                        pnlPercent = null,
                        riskRewardActual = null,
                        marketConditions = ""
                    ))
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF41), contentColor = Color.Black)
            ) {
                Text("[SAVE TRADE]", fontWeight = FontWeight.Bold, fontFamily = com.cryptodept.ui.theme.JetBrainsMono)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
