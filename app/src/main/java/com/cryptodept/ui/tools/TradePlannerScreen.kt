package com.cryptodept.ui.tools

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
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
import com.cryptodept.domain.model.SetupVerdict
import com.cryptodept.domain.model.TradeDirectionType
import com.cryptodept.domain.model.TradeSetup
import com.cryptodept.ui.theme.*
import com.cryptodept.viewmodel.TradePlannerViewModel
import com.cryptodept.ui.components.TerminalInput
import java.util.Locale

@Composable
fun TradePlannerScreen(
    viewModel: TradePlannerViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onSendToSizer: (Double, Double, Double) -> Unit = { _, _, _ -> }
) {
    val colors = LocalTerminalColors.current
    val direction by viewModel.direction.collectAsState()
    val entryPrice by viewModel.entryPrice.collectAsState()
    val stopLoss by viewModel.stopLoss.collectAsState()
    val takeProfit by viewModel.takeProfit.collectAsState()
    val setup by viewModel.setup.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = ">>> TRADE PLANNER — Pre-Trade Analysis",
            color = colors.primary,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ИЗБОР НА ПОСОКА
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("DIRECTION: ", color = colors.dimText, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            TradeDirectionToggle(direction) { viewModel.setDirection(it) }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ПОЛЕТА ЗА ВЪВЕЖДАНЕ С ФОРМАТИРАНЕ
        TerminalInput(
            label = "ENTRY",
            value = if (entryPrice == 0.0) "" else String.format(Locale.US, "%.4f", entryPrice),
            onValueChange = { it.toDoubleOrNull()?.let { v -> viewModel.setEntryPrice(v) } }
        )

        TerminalInput(
            label = "STOP LOSS",
            value = if (stopLoss == 0.0) "" else String.format(Locale.US, "%.4f", stopLoss),
            onValueChange = { it.toDoubleOrNull()?.let { v -> viewModel.setStopLoss(v) } }
        )

        TerminalInput(
            label = "TAKE PROFIT",
            value = if (takeProfit == 0.0) "" else String.format(Locale.US, "%.4f", takeProfit),
            onValueChange = { it.toDoubleOrNull()?.let { v -> viewModel.setTakeProfit(v) } }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.analyzeSetup() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.background),
            shape = RectangleShape,
            enabled = !isLoading
        ) {
            Text(if (isLoading) "ANALYZING..." else "ANALYZE SETUP", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // РЕЗУЛТАТИ ОТ АНАЛИЗА
        setup?.let { res ->
            VerdictView(res)
            Spacer(modifier = Modifier.height(24.dp))
            ChecklistView(res)

            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onSendToSizer(res.entryPrice, res.stopLoss, res.takeProfit) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.amber, contentColor = colors.background),
                    shape = RectangleShape
                ) {
                    Text("SEND TO SIZER", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { /* Запазване */ },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, colors.primary),
                    shape = RectangleShape
                ) {
                    Text("SAVE TO JOURNAL", fontSize = 11.sp, color = colors.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("< BACK_TO_TOOLS", color = colors.primary, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun TradeDirectionToggle(selected: TradeDirectionType, onSelect: (TradeDirectionType) -> Unit) {
    val colors = LocalTerminalColors.current
    Row(modifier = Modifier.border(1.dp, colors.grid)) {
        Box(
            modifier = Modifier
                .background(if (selected == TradeDirectionType.LONG) colors.primary else Color.Transparent)
                .clickable { onSelect(TradeDirectionType.LONG) }
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text("LONG", color = if (selected == TradeDirectionType.LONG) colors.background else colors.primary, fontSize = 12.sp)
        }
        Box(
            modifier = Modifier
                .background(if (selected == TradeDirectionType.SHORT) colors.danger else Color.Transparent)
                .clickable { onSelect(TradeDirectionType.SHORT) }
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text("SHORT", color = if (selected == TradeDirectionType.SHORT) colors.background else colors.danger, fontSize = 12.sp)
        }
    }
}

@Composable
fun VerdictView(setup: TradeSetup) {
    val colors = LocalTerminalColors.current
    val color = when (setup.verdict) {
        SetupVerdict.STRONG_SETUP -> colors.primary
        SetupVerdict.GOOD_SETUP -> colors.primary.copy(alpha = 0.7f)
        SetupVerdict.PROCEED_CAUTION -> colors.amber
        SetupVerdict.AVOID -> colors.danger
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, color)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("VERDICT: ${setup.verdict.name.replace("_", " ")}", color = color, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Text("${setup.score}/${setup.maxScore} checks passed", color = color.copy(alpha = 0.7f), fontSize = 12.sp)
    }
}

@Composable
fun ChecklistView(setup: TradeSetup) {
    val colors = LocalTerminalColors.current
    val categories = setup.checklist.groupBy { it.category }

    categories.forEach { (category, items) ->
        Text(category, color = colors.dimText, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))

        val hasCriticalFail = items.any { it.isCritical && !it.isPassed }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, if (hasCriticalFail) colors.danger else colors.grid)
                .padding(8.dp)
        ) {
            items.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Icon(
                        imageVector = if (item.isPassed) Icons.Default.CheckCircle else if (item.isCritical) Icons.Default.Close else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (item.isPassed) colors.primary else if (item.isCritical) colors.danger else colors.amber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.description + if (item.isCritical && !item.isPassed) " ⚠" else "",
                        color = if (item.isPassed) colors.textPrimary else if (item.isCritical) colors.danger else colors.amber,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
