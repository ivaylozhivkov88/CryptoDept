package com.cryptodept.ui.tools

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
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
import com.cryptodept.domain.model.PositionGrade
import com.cryptodept.domain.model.PositionSizeResult
import com.cryptodept.ui.theme.*
import com.cryptodept.viewmodel.PositionSizeViewModel
import com.cryptodept.ui.components.TerminalCard
import com.cryptodept.ui.components.TerminalInput
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PositionSizeScreen(
    viewModel: PositionSizeViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val colors = LocalTerminalColors.current
    val portfolioSize by viewModel.portfolioSize.collectAsState()
    val riskPercent by viewModel.riskPercent.collectAsState()
    val entryPrice by viewModel.entryPrice.collectAsState()
    val stopLoss by viewModel.stopLoss.collectAsState()
    val takeProfit by viewModel.takeProfit.collectAsState()
    val result by viewModel.result.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = ">>> POSITION SIZER — Risk-Based Calculator",
            color = colors.primary,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // INPUT SECTION
        TerminalCard(title = "PORTFOLIO & RISK") {
            TerminalInput(
                label = "PORTFOLIO SIZE (USD)",
                value = portfolioSize.toString(),
                onValueChange = { viewModel.setPortfolioSize(it) }
            )
            TerminalInput(
                label = "RISK PER TRADE (%)",
                value = riskPercent.toString(),
                onValueChange = { viewModel.setRiskPercent(it) }
            )
            result?.let {
                Text(
                    text = "MAX LOSS: $${formatNumber(it.maxLossUsd)}",
                    color = colors.amber,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TerminalCard(title = "TRADE PARAMETERS") {
            TerminalInput(
                label = "ENTRY PRICE",
                value = entryPrice.toString(),
                onValueChange = { viewModel.setEntryPrice(it) },
                trailingIcon = {
                    TextButton(onClick = { viewModel.useCurrentPrice() }) {
                        Text("[USE CURRENT]", color = colors.primary, fontSize = 10.sp)
                    }
                }
            )
            TerminalInput(
                label = "STOP LOSS",
                value = stopLoss.toString(),
                onValueChange = { viewModel.setStopLoss(it) }
            )
            TerminalInput(
                label = "TAKE PROFIT",
                value = takeProfit.toString(),
                onValueChange = { viewModel.setTakeProfit(it) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // RESULT SECTION
        result?.let { res ->
            CalculationResultView(res)
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = { viewModel.saveToJournal() },
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, colors.primary),
            shape = RectangleShape
        ) {
            Text("[SAVE AS TRADE JOURNAL ENTRY]", color = colors.primary, fontFamily = FontFamily.Monospace)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("< BACK_TO_TOOLS", color = colors.primary, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun CalculationResultView(res: PositionSizeResult) {
    val colors = LocalTerminalColors.current
    val gradeColor = Color(res.grade.color)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, gradeColor)
            .background(gradeColor.copy(alpha = 0.05f))
            .padding(16.dp)
    ) {
        Text("═══════ CALCULATION RESULT ═══════", color = gradeColor, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(16.dp))

        ResultRow("Position Size:", "${formatNumber(res.positionSizeCoins)} Coins")
        ResultRow("Value (USD):", "$${formatNumber(res.positionSizeUsd)}")
        ResultRow("Leverage:", "${String.format("%.1fx", res.leverageNeeded)} ${if (res.leverageNeeded <= 1.0) "(NO LEVERAGE)" else ""}")

        HorizontalDivider(color = gradeColor.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))

        ResultRow("Risk:Reward:", "1:${String.format("%.1f", res.riskRewardRatio)}  ${res.grade.label}")
        ResultRow("Potential Gain:", "+$${formatNumber(res.potentialGainUsd)} (+${String.format("%.1f", res.distanceToTPPercent)}%)", colors.primary)
        ResultRow("Potential Loss:", "-$${formatNumber(res.potentialLossUsd)} (-${String.format("%.1f", res.distanceToSLPercent)}%)", colors.danger)

        Spacer(modifier = Modifier.height(16.dp))

        // Risk adjustment box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.textPrimary.copy(alpha = 0.05f))
                .padding(8.dp)
        ) {
            Column {
                Text("⚠ RISK ADJUSTMENT", color = colors.amber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("Recommended Size: $${formatNumber(res.riskAdjustedSize)}", color = colors.textPrimary, fontSize = 12.sp)
                Text("Reason: ${res.riskAdjustmentReason}", color = colors.dimText, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun ResultRow(label: String, value: String, valueColor: Color? = null) {
    val colors = LocalTerminalColors.current
    val actualValueColor = valueColor ?: colors.textPrimary
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = colors.dimText, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = actualValueColor, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

fun formatNumber(value: Double): String {
    return try {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        formatter.maximumFractionDigits = if (value < 1.0) 6 else 2
        formatter.format(value)
    } catch (e: Exception) {
        value.toString()
    }
}
