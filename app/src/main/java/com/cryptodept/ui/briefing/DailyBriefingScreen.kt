package com.cryptodept.ui.briefing

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.domain.usecase.DailyBriefingGenerator
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.viewmodel.BriefingUiState
import com.cryptodept.viewmodel.BriefingViewModel

@Composable
fun DailyBriefingScreen(viewModel: BriefingViewModel = hiltViewModel()) {
    val state by viewModel.briefingState.collectAsState()
    val context = LocalContext.current
    val colors = LocalTerminalColors.current

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(16.dp),
    ) {
        when (val uiState = state) {
            is BriefingUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = colors.primary,
                )
            }
            is BriefingUiState.Error -> {
                Text(
                    text = ">>> ERROR: ${uiState.message}",
                    color = colors.error,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            is BriefingUiState.Success -> {
                Column {
                    Box(modifier = Modifier.weight(1f)) {
                        BriefingContent(uiState.briefing) {
                            viewModel.generateBriefing()
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val prompt = generateGlobalAiPrompt(uiState.briefing)
                            val sendIntent =
                                Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, prompt)
                                    type = "text/plain"
                                }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Market Briefing"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.background),
                        shape = RectangleShape,
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("SHARE GLOBAL VIDEO PROMPT", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

private fun generateGlobalAiPrompt(briefing: DailyBriefingGenerator.DailyBriefing): String {
    val date = briefing.date
    val risk = briefing.riskScore.level.label
    val sentence = briefing.marketSentence

    return "COMMAND: Create a 5-second STATIC cinematic video with NO camera movement and NO text animation. " +
        "SCENE: A high-tech global market monitoring terminal. " +
        "ATMOSPHERE: Cyberpunk data center, neon green and amber lighting. " +
        "TEXT INSTRUCTION: Display the following text on a central holographic screen EXACTLY as written: " +
        "\"GLOBAL MARKET STATUS\" " +
        "\"$date\" " +
        "\"MARKET RISK: $risk\" " +
        "\"${sentence.take(100)}...\" " +
        "\"TRADING BIAS: ${briefing.tradingSuggestion.take(50)}...\". " +
        "VISUALS: Center the text. Use bold glowing neon fonts. English language only. Perfectly legible and still."
}

@Composable
fun BriefingContent(
    briefing: DailyBriefingGenerator.DailyBriefing,
    onRefresh: () -> Unit,
) {
    val colors = LocalTerminalColors.current
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .border(1.dp, colors.grid, RectangleShape),
    ) {
        item {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = ">>> DAILY MARKET BRIEFING",
                    color = colors.primary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                Button(
                    onClick = onRefresh,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = colors.primary,
                        ),
                    shape = RectangleShape,
                    modifier = Modifier.border(1.dp, colors.primary, RectangleShape),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text("[REGENERATE]", fontFamily = FontFamily.Monospace)
                }
            }
            Text(
                text = briefing.date,
                color = colors.primary.copy(alpha = 0.7f),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            HorizontalDivider(color = colors.grid, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
        }

        item {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = "MARKET SUMMARY:",
                    color = colors.amber,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = briefing.marketSentence,
                    color = colors.textPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            HorizontalDivider(color = colors.grid, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
        }

        item {
            Text(
                text = "KEY METRICS:",
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(briefing.keyMetrics) { metric ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = metric.label.padEnd(15),
                    color = colors.dimText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                )
                Text(
                    text = metric.value,
                    color = colors.textPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                )
                Text(
                    text = metric.change ?: "",
                    color = if (metric.sentiment == "BULLISH") colors.primary else colors.error,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                )
                Text(
                    text = metric.sentiment,
                    color =
                        when (metric.sentiment) {
                            "BULLISH" -> colors.primary
                            "BEARISH" -> colors.error
                            else -> colors.amber
                        },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                )
            }
        }

        item {
            HorizontalDivider(color = colors.grid, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = "⚠ ALERTS (${briefing.topAlerts.size}):",
                color = colors.amber,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(briefing.topAlerts) { alert ->
            val color =
                when (alert.severity) {
                    DailyBriefingGenerator.AlertSeverity.CRITICAL -> colors.error
                    DailyBriefingGenerator.AlertSeverity.WARNING -> colors.amber
                    DailyBriefingGenerator.AlertSeverity.INFO -> colors.primary
                }
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .border(0.5.dp, color, RectangleShape)
                        .padding(8.dp),
            ) {
                Text(
                    text = "[${alert.severity.name}] ${alert.title}",
                    color = color,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
                Text(
                    text = alert.detail,
                    color = color.copy(alpha = 0.8f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                )
            }
        }

        item {
            HorizontalDivider(color = colors.grid, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = "RECOMMENDATION:",
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Text(
                text = briefing.tradingSuggestion,
                color = colors.textPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                modifier = Modifier.padding(8.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
