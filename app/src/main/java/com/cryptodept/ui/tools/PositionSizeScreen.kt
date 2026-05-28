package com.cryptodept.ui.tools

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import com.cryptodept.domain.model.PositionSizeResult
import com.cryptodept.ui.components.ComposeErrorBoundary
import com.cryptodept.ui.components.TerminalCard
import com.cryptodept.ui.components.TerminalInput
import com.cryptodept.ui.components.SanityCheckDialog
import com.cryptodept.ui.components.SanitySeverity
import com.cryptodept.ui.theme.*
import com.cryptodept.util.TerminalConfig
import com.cryptodept.viewmodel.PositionSizeViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PositionSizeScreen(
    viewModel: PositionSizeViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val colors = LocalTerminalColors.current
    ComposeErrorBoundary {
        PositionSizeContent(colors, viewModel, onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PositionSizeContent(
    colors: TerminalColorSet,
    viewModel: PositionSizeViewModel,
    onBack: () -> Unit,
) {
    val portfolioSize by viewModel.portfolioSize.collectAsState()
    val riskPercent by viewModel.riskPercent.collectAsState()
    val entryPrice by viewModel.entryPrice.collectAsState()
    val stopLoss by viewModel.stopLoss.collectAsState()
    val takeProfit by viewModel.takeProfit.collectAsState()
    val result by viewModel.result.collectAsState()

    var showSanityCheck by remember { mutableStateOf(false) }
    var sanityCheckSeverity by remember { mutableStateOf(SanitySeverity.WARNING) }
    var sanityCheckMessage by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    if (showSanityCheck) {
        SanityCheckDialog(
            title = "LARGE_POSITION_DETECTED",
            message = sanityCheckMessage,
            severity = sanityCheckSeverity,
            onConfirm = {
                showSanityCheck = false
            },
            onDismiss = { 
                showSanityCheck = false 
            },
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(TerminalConfig.UI.DEFAULT_PADDING)
                .verticalScroll(scrollState),
    ) {
        Text(
            text = ">>> POSITION SIZER — Risk-Based Calculator",
            color = colors.primary,
            fontFamily = FontFamily.Monospace,
            fontSize = TerminalConfig.UI.FONT_SIZE_MEDIUM,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(TerminalConfig.UI.SPACER_LARGE))

        // INPUT SECTION
        TerminalCard(title = "PORTFOLIO & RISK") {
            TerminalInput(
                label = "PORTFOLIO SIZE (USD)",
                value = portfolioSize,
                onValueChange = { viewModel.setPortfolioSize(it) },
            )
            TerminalInput(
                label = "RISK PER TRADE (%)",
                value = riskPercent,
                onValueChange = { viewModel.setRiskPercent(it) },
            )
            result?.let {
                Text(
                    text = "MAX LOSS: $${formatNumber(it.maxLossUsd)}",
                    color = colors.amber,
                    fontFamily = FontFamily.Monospace,
                    fontSize = TerminalConfig.UI.FONT_SIZE_NORMAL,
                    modifier = Modifier.padding(top = TerminalConfig.UI.SMALL_PADDING),
                )
            }
        }

        Spacer(modifier = Modifier.height(TerminalConfig.UI.SPACER_LARGE))

        TerminalCard(title = "TRADE PARAMETERS") {
            TerminalInput(
                label = "ENTRY PRICE",
                value = entryPrice,
                onValueChange = { viewModel.setEntryPrice(it) },
                trailingIcon = {
                    TextButton(onClick = { viewModel.useCurrentPrice() }) {
                        Text(
                            text = TerminalConfig.Strings.USE_CURRENT,
                            color = colors.primary,
                            fontSize = TerminalConfig.UI.FONT_SIZE_MICRO
                        )
                    }
                },
            )
            TerminalInput(
                label = "STOP LOSS",
                value = stopLoss,
                onValueChange = { viewModel.setStopLoss(it) },
            )
            TerminalInput(
                label = "TAKE PROFIT",
                value = takeProfit,
                onValueChange = { viewModel.setTakeProfit(it) },
            )
        }

        Spacer(modifier = Modifier.height(TerminalConfig.UI.SPACER_LARGE))

        // CALCULATION ACTION (NEW)
        Button(
            onClick = { 
                val currentResult = result
                val portfolio = portfolioSize.toDoubleOrNull() ?: 0.0
                if (currentResult != null && portfolio > 0) {
                    val percentOfAccount = (currentResult.positionSizeUsd / portfolio) * 100
                    
                    when {
                        percentOfAccount > 50 -> {
                            sanityCheckSeverity = SanitySeverity.CRITICAL
                            sanityCheckMessage = """
                                This trade would risk ${percentOfAccount.toInt()}% of your portfolio.
                                
                                Professional traders rarely risk more than 1-3% per trade.
                                A single bad trade could destroy your account.
                                
                                Are you absolutely sure?
                            """.trimIndent()
                            showSanityCheck = true
                        }
                        percentOfAccount > 30 -> {
                            sanityCheckSeverity = SanitySeverity.WARNING
                            sanityCheckMessage = """
                                This trade risks ${percentOfAccount.toInt()}% of your portfolio.
                                
                                Most experienced traders limit individual trade risk to 1-3%.
                                Larger positions reduce your ability to survive losing streaks.
                                
                                Consider a smaller position size.
                            """.trimIndent()
                            showSanityCheck = true
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.background),
            shape = RectangleShape
        ) {
            Text("RUN SANITY CHECK", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }

        Spacer(modifier = Modifier.height(TerminalConfig.UI.SPACER_LARGE))

        // RESULT SECTION
        if (result != null) {
            CalculationResultView(result!!)
        } else {
            // Placeholder for Guided Tour
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .border(1.dp, colors.grid, RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ">>> AWAITING_CALCULATION_PARAMETERS",
                    color = colors.dimText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(TerminalConfig.UI.SPACER_LARGE + TerminalConfig.UI.SPACER_MEDIUM))

        OutlinedButton(
            onClick = { viewModel.saveToJournal() },
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(TerminalConfig.UI.BORDER_WIDTH, colors.primary),
            shape = RectangleShape,
        ) {
            Text("[SAVE AS TRADE JOURNAL ENTRY]", color = colors.primary, fontFamily = FontFamily.Monospace)
        }

        Spacer(modifier = Modifier.height(TerminalConfig.UI.SPACER_LARGE))

        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(TerminalConfig.Strings.BACK_TO_TOOLS, color = colors.primary, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun CalculationResultView(res: PositionSizeResult) {
    val colors = LocalTerminalColors.current
    val gradeColor = if (res.grade == com.cryptodept.domain.model.PositionGrade.POOR || res.grade == com.cryptodept.domain.model.PositionGrade.INVALID) colors.error else colors.primary

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(TerminalConfig.UI.BORDER_WIDTH, gradeColor)
                .background(gradeColor.copy(alpha = 0.05f))
                .padding(TerminalConfig.UI.DEFAULT_PADDING),
    ) {
        Text("═══════ CALCULATION RESULT ═══════", color = gradeColor, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.height(TerminalConfig.UI.SPACER_LARGE))

        ResultRow("Position Size:", "${formatNumber(res.positionSizeCoins)} Coins")
        ResultRow("Value (USD):", "$${formatNumber(res.positionSizeUsd)}")
        ResultRow("Leverage:", "${String.format("%.1fx", res.leverageNeeded)} ${if (res.leverageNeeded <= 1.0) "(NO LEVERAGE)" else ""}")

        HorizontalDivider(
            color = gradeColor.copy(alpha = 0.2f),
            modifier = Modifier.padding(vertical = TerminalConfig.UI.SPACER_MEDIUM + TerminalConfig.UI.SPACER_SMALL)
        )

        ResultRow("Risk:Reward:", "1:${String.format("%.1f", res.riskRewardRatio)}  ${res.grade.label}")
        ResultRow(
            "Potential Gain:",
            "+$${formatNumber(res.potentialGainUsd)} (+${String.format("%.1f", res.distanceToTPPercent)}%)",
            colors.primary,
        )
        ResultRow(
            "Potential Loss:",
            "-$${formatNumber(res.potentialLossUsd)} (-${String.format("%.1f", res.distanceToSLPercent)}%)",
            colors.danger,
        )

        Spacer(modifier = Modifier.height(TerminalConfig.UI.SPACER_LARGE))

        // Risk adjustment box
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(colors.textPrimary.copy(alpha = 0.05f))
                    .padding(TerminalConfig.UI.SMALL_PADDING),
        ) {
            Column {
                Text("⚠ RISK ADJUSTMENT", color = colors.amber, fontSize = TerminalConfig.UI.FONT_SIZE_SMALL, fontWeight = FontWeight.Bold)
                Text("Recommended Size: $${formatNumber(res.riskAdjustedSize)}", color = colors.textPrimary, fontSize = TerminalConfig.UI.FONT_SIZE_NORMAL)
                Text("Reason: ${res.riskAdjustmentReason}", color = colors.dimText, fontSize = TerminalConfig.UI.FONT_SIZE_MICRO)
            }
        }
    }
}

@Composable
fun ResultRow(
    label: String,
    value: String,
    valueColor: Color? = null,
) {
    val colors = LocalTerminalColors.current
    val actualValueColor = valueColor ?: colors.textPrimary
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = TerminalConfig.UI.SPACER_SMALL / 2),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = colors.dimText, fontSize = TerminalConfig.UI.FONT_SIZE_NORMAL, fontFamily = FontFamily.Monospace)
        Text(value, color = actualValueColor, fontSize = TerminalConfig.UI.FONT_SIZE_NORMAL, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

fun formatNumber(value: Double): String =
    try {
        val formatter = NumberFormat.getNumberInstance(Locale.US)
        formatter.maximumFractionDigits = if (value < 1.0) 6 else 2
        formatter.format(value)
    } catch (e: Exception) {
        value.toString()
    }
