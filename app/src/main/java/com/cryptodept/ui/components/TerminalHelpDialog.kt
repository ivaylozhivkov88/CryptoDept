package com.cryptodept.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.ui.theme.LocalTerminalColors

@Composable
fun TerminalHelpDialog(onDismiss: () -> Unit) {
    val colors = LocalTerminalColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.background,
        modifier = Modifier.border(1.dp, colors.primary, RectangleShape),
        title = {
            Text(
                ">>> SYSTEM_COMMAND_INDEX",
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
            ) {
                CommandSection(
                    "CORE NAVIGATION",
                    listOf(
                        "DASHBOARD" to "Return to main terminal",
                        "MARKETS" to "View global asset list",
                        "NEWS" to "Global crypto news stream",
                        "ALERTS" to "System alerts log",
                        "SETTINGS" to "Terminal configuration",
                        "BACK" to "Navigate to previous state",
                    ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                CommandSection(
                    "ASSET ANALYSIS",
                    listOf(
                        "CHART [SYM]" to "Open candlestick chart (e.g. CHART BTC)",
                        "ANALYSIS [SYM]" to "Deep technical breakdown",
                        "COMPARE [S1] [S2]" to "Side-by-side asset comparison",
                        "MATRIX" to "Asset correlation heat map",
                        "PREDICT" to "AI Ensemble forecasting engine",
                        "INDICATORS" to "Global technical scanner",
                    ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                CommandSection(
                    "TRADING TOOLS",
                    listOf(
                        "SIZER" to "Position size & leverage calculator",
                        "PLANNER" to "Risk/Reward trade architect",
                        "ENTRY" to "Optimal entry point analyzer",
                        "MTF" to "Multi-timeframe confluence scan",
                        "JOURNAL" to "Trading performance log",
                        "RISK" to "Portfolio risk metrics dashboard",
                    ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                CommandSection(
                    "INTELLIGENCE",
                    listOf(
                        "COACH" to "AI Trading Mentor (Gemini)",
                        "BRIEF" to "Daily AI market briefing",
                        "MACRO" to "Global macro correlations",
                        "DERIVS" to "Futures & Options market data",
                    ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                CommandSection(
                    "SYSTEM",
                    listOf(
                        "HELP" to "Show this command index",
                        "VERSION" to "System build information",
                        "CLEAR" to "Clear terminal input",
                        "LOGOUT" to "De-authorize admin session",
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = colors.primary, fontFamily = FontFamily.Monospace)
            }
        },
    )
}

@Composable
private fun CommandSection(
    title: String,
    commands: List<Pair<String, String>>,
) {
    val colors = LocalTerminalColors.current
    Column {
        Text(
            text = "--- $title ---",
            color = colors.amber,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        commands.forEach { (cmd, desc) ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Text(
                    text = cmd.padEnd(14),
                    color = colors.primary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(100.dp),
                )
                Text(
                    text = desc,
                    color = colors.dimText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
