package com.cryptodept.ui.risk

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.domain.usecase.RiskScoreEngine
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.viewmodel.RiskUiState
import com.cryptodept.viewmodel.RiskViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RiskScoreScreen(viewModel: RiskViewModel = hiltViewModel()) {
    val state by viewModel.riskState.collectAsState()
    val colors = LocalTerminalColors.current

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(16.dp),
    ) {
        when (val uiState = state) {
            is RiskUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = colors.primary,
                )
            }
            is RiskUiState.Error -> {
                Text(
                    text = ">>> ERROR: ${uiState.message}",
                    color = colors.error,
                    fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            is RiskUiState.Success -> {
                RiskContent(uiState.score, uiState.btcPrice) {
                    viewModel.calculateRisk()
                }
            }
        }
    }
}

@Composable
fun RiskContent(
    score: RiskScoreEngine.RiskScore,
    btcPrice: Double,
    onRefresh: () -> Unit,
) {
    val colors = LocalTerminalColors.current
    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(score.calculatedAt))

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .border(1.dp, colors.primary, RectangleShape),
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
                    text = ">>> RISK ENGINE",
                    color = colors.primary,
                    fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = onRefresh,
                    border = BorderStroke(1.dp, colors.primary),
                    shape = RectangleShape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("REFRESH", fontFamily = com.cryptodept.ui.theme.JetBrainsMono, fontSize = 10.sp)
                }
            }
            Text(
                text = "Last calculated: $timeStr",
                color = colors.primary.copy(alpha = 0.7f),
                fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            HorizontalDivider(color = colors.primary, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
        }

        item {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "OVERALL RISK SCORE",
                    color = colors.textPrimary,
                    fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                    fontSize = 18.sp,
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Progress Bar
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .border(1.dp, colors.grid, RectangleShape),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth(score.overall / 100f)
                                .fillMaxHeight()
                                .background(Color(score.level.color)),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("LOW", color = colors.dimText, fontSize = 10.sp, fontFamily = com.cryptodept.ui.theme.JetBrainsMono)
                    Text("MODERATE", color = colors.dimText, fontSize = 10.sp, fontFamily = com.cryptodept.ui.theme.JetBrainsMono)
                    Text("HIGH", color = colors.dimText, fontSize = 10.sp, fontFamily = com.cryptodept.ui.theme.JetBrainsMono)
                    Text("EXTREME", color = colors.dimText, fontSize = 10.sp, fontFamily = com.cryptodept.ui.theme.JetBrainsMono)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${score.overall}/100",
                    color = Color(score.level.color),
                    fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "⚠ ${score.level.label} — ${score.recommendation}",
                    color = Color(score.level.color),
                    fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
            HorizontalDivider(color = colors.primary, thickness = 1.dp)
        }

        item {
            Text(
                text = "COMPONENT BREAKDOWN",
                color = colors.primary,
                fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                modifier = Modifier.padding(8.dp),
            )
        }

        items(score.components) { component ->
            RiskComponentRow(component)
        }

        item {
            HorizontalDivider(color = colors.primary, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = "DOMINANT RISK FACTORS:",
                color = colors.primary,
                fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            score.dominantFactors.forEach { factor ->
                Text(
                    text = "> $factor",
                    color = colors.textPrimary,
                    fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun RiskComponentRow(component: RiskScoreEngine.RiskComponent) {
    val colors = LocalTerminalColors.current
    val barColor =
        when {
            component.score < 40 -> colors.primary
            component.score < 70 -> colors.amber
            else -> colors.error
        }

    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = component.name.padEnd(16),
                color = colors.textPrimary,
                fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                fontSize = 12.sp,
            )
            Text(
                text = if (component.isBearish) "BEARISH" else "NEUTRAL/BULLISH",
                color = if (component.isBearish) colors.error else colors.primary,
                fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                fontSize = 10.sp,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(8.dp)
                        .border(0.5.dp, colors.grid, RectangleShape),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(component.score / 100f)
                            .fillMaxHeight()
                            .background(barColor),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = component.score.toString().padStart(3),
                color = barColor,
                fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                fontSize = 12.sp,
            )
        }
        Text(
            text = component.signal,
            color = colors.dimText,
            fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
            fontSize = 10.sp,
        )
    }
}
