package com.cryptodept.ui.strategy

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.cryptodept.domain.model.StrategyRule
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.viewmodel.StrategyViewModel
import java.util.Locale

@Composable
fun StrategyScreen(
    onBack: () -> Unit,
    viewModel: StrategyViewModel = hiltViewModel()
) {
    val colors = LocalTerminalColors.current
    val entryRules by viewModel.entryRules.collectAsState()
    val exitRules by viewModel.exitRules.collectAsState()
    val report by viewModel.backtestReport.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    var showRuleDialog by remember { mutableStateOf(false) }
    var ruleType by remember { mutableStateOf("ENTRY") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp)
    ) {
        Header(onBack)

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                RuleSection("ENTRY_CONDITIONS", entryRules) {
                    ruleType = "ENTRY"
                    showRuleDialog = true
                }
            }
            item {
                RuleSection("EXIT_CONDITIONS", exitRules) {
                    ruleType = "EXIT"
                    showRuleDialog = true
                }
            }
            if (report != null) {
                item {
                    BacktestReportView(report!!)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { viewModel.clearRules() },
                modifier = Modifier.weight(1f),
                shape = RectangleShape,
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = androidx.compose.ui.graphics.SolidColor(colors.danger))
            ) {
                Text("CLEAR", color = colors.danger, fontFamily = FontFamily.Monospace)
            }
            Button(
                onClick = { viewModel.runBacktest() },
                modifier = Modifier.weight(1f),
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                enabled = !isProcessing && entryRules.isNotEmpty()
            ) {
                Text(if (isProcessing) "RUNNING..." else "START_BACKTEST", color = Color.Black, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showRuleDialog) {
        AddRuleDialog(
            onDismiss = { showRuleDialog = false },
            onAdd = { rule ->
                if (ruleType == "ENTRY") viewModel.addEntryRule(rule)
                else viewModel.addExitRule(rule)
                showRuleDialog = false
            }
        )
    }
}

@Composable
fun Header(onBack: () -> Unit) {
    val colors = LocalTerminalColors.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(">>> STRATEGY_BUILDER_v1", color = colors.primary, fontFamily = FontFamily.Monospace, fontSize = 18.sp)
        IconButton(onClick = onBack) {
            Text("[X]", color = colors.danger, fontFamily = FontFamily.Monospace)
        }
    }
    HorizontalDivider(color = colors.grid, modifier = Modifier.padding(vertical = 12.dp))
}

@Composable
fun RuleSection(title: String, rules: List<StrategyRule>, onAdd: () -> Unit) {
    val colors = LocalTerminalColors.current
    Column(modifier = Modifier.fillMaxWidth().border(1.dp, colors.grid).padding(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = colors.amber, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            TextButton(onClick = onAdd) {
                Text("[+ ADD]", color = colors.primary, fontFamily = FontFamily.Monospace)
            }
        }
        if (rules.isEmpty()) {
            Text("NO RULES DEFINED", color = colors.dimText, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(vertical = 4.dp))
        } else {
            rules.forEach { rule ->
                Text("IF ${rule.indicator} ${rule.operator} ${rule.value}", color = colors.textPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun BacktestReportView(report: com.cryptodept.domain.usecase.BacktestReport) {
    val colors = LocalTerminalColors.current
    Column(modifier = Modifier.fillMaxWidth().border(1.dp, colors.primary).padding(16.dp)) {
        Text("--- BACKTEST_REPORT ---", color = colors.primary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        ReportLine("TOTAL_RETURN", "${String.format(Locale.US, "%.2f", report.totalReturnPercent)}%", if (report.totalReturnPercent >= 0) colors.primary else colors.danger)
        ReportLine("WIN_RATE", "${String.format(Locale.US, "%.1f", report.winRate)}%")
        ReportLine("TRADES_COUNT", "${report.tradesCount}")
        ReportLine("MAX_DRAWDOWN", "-${String.format(Locale.US, "%.2f", report.maxDrawdownPercent)}%", colors.danger)
        ReportLine("FINAL_BALANCE", "$${String.format(Locale.US, "%.2f", report.finalBalance)}")
    }
}

@Composable
fun ReportLine(label: String, value: String, color: Color = LocalTerminalColors.current.textPrimary) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = LocalTerminalColors.current.dimText, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = color, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AddRuleDialog(onDismiss: () -> Unit, onAdd: (StrategyRule) -> Unit) {
    val colors = LocalTerminalColors.current
    var indicator by remember { mutableStateOf("RSI") }
    var operator by remember { mutableStateOf("<") }
    var value by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.background,
        title = { Text("ADD_STRATEGY_RULE", color = colors.primary, fontFamily = FontFamily.Monospace) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("INDICATOR", color = colors.dimText, fontSize = 10.sp)
                IndicatorSelector(indicator) { indicator = it }
                Text("OPERATOR", color = colors.dimText, fontSize = 10.sp)
                OperatorSelector(operator) { operator = it }
                Text("VALUE", color = colors.dimText, fontSize = 10.sp)
                TextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(unfocusedContainerColor = colors.surface, focusedContainerColor = colors.surface)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { 
                onAdd(StrategyRule(indicator, operator, value.toDoubleOrNull() ?: 0.0))
            }) {
                Text("CONFIRM", color = colors.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = colors.danger)
            }
        }
    )
}

@Composable
fun IndicatorSelector(selected: String, onSelect: (String) -> Unit) {
    val indicators = listOf("RSI", "PRICE", "FEAR_GREED", "RISK_SCORE", "FUNDING_RATE")
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        indicators.forEach { ind ->
            FilterChip(
                selected = selected == ind,
                onClick = { onSelect(ind) },
                label = { Text(ind, fontSize = 10.sp) }
            )
        }
    }
}

@Composable
fun OperatorSelector(selected: String, onSelect: (String) -> Unit) {
    val operators = listOf("<", ">", "==", "<=", ">=")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        operators.forEach { op ->
            FilterChip(
                selected = selected == op,
                onClick = { onSelect(op) },
                label = { Text(op) }
            )
        }
    }
}
