package com.cryptodept.ui.risk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.domain.usecase.RiskScoreEngine
import com.cryptodept.viewmodel.RiskUiState
import com.cryptodept.viewmodel.RiskViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RiskScoreScreen(viewModel: RiskViewModel = hiltViewModel()) {
    val state by viewModel.riskState.collectAsState()

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp),
    ) {
        when (val uiState = state) {
            is RiskUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF00FF41),
                )
            }
            is RiskUiState.Error -> {
                Text(
                    text = ">>> ERROR: ${uiState.message}",
                    color = Color.Red,
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
    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(score.calculatedAt))

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .border(1.dp, Color(0xFF00FF41), RectangleShape),
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
                    text = ">>> RISK ASSESSMENT ENGINE",
                    color = Color(0xFF00FF41),
                    fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                Button(
                    onClick = onRefresh,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color(0xFF00FF41),
                        ),
                    shape = RectangleShape,
                    modifier = Modifier.border(1.dp, Color(0xFF00FF41), RectangleShape),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text("[REFRESH]", fontFamily = com.cryptodept.ui.theme.JetBrainsMono)
                }
            }
            Text(
                text = "Last calculated: $timeStr",
                color = Color(0xFF00FF41).copy(alpha = 0.7f),
                fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            HorizontalDivider(color = Color(0xFF00FF41), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
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
                    color = Color.White,
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
                            .border(1.dp, Color.Gray, RectangleShape),
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
                    Text("LOW", color = Color.Gray, fontSize = 10.sp, fontFamily = com.cryptodept.ui.theme.JetBrainsMono)
                    Text("MODERATE", color = Color.Gray, fontSize = 10.sp, fontFamily = com.cryptodept.ui.theme.JetBrainsMono)
                    Text("HIGH", color = Color.Gray, fontSize = 10.sp, fontFamily = com.cryptodept.ui.theme.JetBrainsMono)
                    Text("EXTREME", color = Color.Gray, fontSize = 10.sp, fontFamily = com.cryptodept.ui.theme.JetBrainsMono)
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
            HorizontalDivider(color = Color(0xFF00FF41), thickness = 1.dp)
        }

        item {
            Text(
                text = "COMPONENT BREAKDOWN",
                color = Color(0xFF00FF41),
                fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                modifier = Modifier.padding(8.dp),
            )
        }

        items(score.components) { component ->
            RiskComponentRow(component)
        }

        item {
            HorizontalDivider(color = Color(0xFF00FF41), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = "DOMINANT RISK FACTORS:",
                color = Color(0xFF00FF41),
                fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            score.dominantFactors.forEach { factor ->
                Text(
                    text = "> $factor",
                    color = Color.White,
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
    val barColor =
        when {
            component.score < 40 -> Color(0xFF00FF41) // Green
            component.score < 70 -> Color(0xFFFFB000) // Amber
            else -> Color(0xFFFF3B30) // Red
        }

    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = component.name.padEnd(16),
                color = Color.White,
                fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                fontSize = 12.sp,
            )
            Text(
                text = if (component.isBearish) "BEARISH" else "NEUTRAL/BULLISH",
                color = if (component.isBearish) Color(0xFFFF3B30) else Color(0xFF00FF41),
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
                        .border(0.5.dp, Color.Gray, RectangleShape),
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
            color = Color.Gray,
            fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
            fontSize = 10.sp,
        )
    }
}
