package com.cryptodept.ui.briefing

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
import com.cryptodept.domain.usecase.DailyBriefingGenerator
import com.cryptodept.viewmodel.BriefingUiState
import com.cryptodept.viewmodel.BriefingViewModel

@Composable
fun DailyBriefingScreen(
    viewModel: BriefingViewModel = hiltViewModel()
) {
    val state by viewModel.briefingState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        when (val uiState = state) {
            is BriefingUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF00FF41)
                )
            }
            is BriefingUiState.Error -> {
                Text(
                    text = ">>> ERROR: ${uiState.message}",
                    color = Color.Red,
                    fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is BriefingUiState.Success -> {
                BriefingContent(uiState.briefing) {
                    viewModel.generateBriefing()
                }
            }
        }
    }
}

@Composable
fun BriefingContent(
    briefing: DailyBriefingGenerator.DailyBriefing,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .border(1.dp, Color(0xFF00FF41), RectangleShape)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ">>> DAILY MARKET BRIEFING",
                    color = Color(0xFF00FF41),
                    fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Button(
                    onClick = onRefresh,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF00FF41)
                    ),
                    shape = RectangleShape,
                    modifier = Modifier.border(1.dp, Color(0xFF00FF41), RectangleShape),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("[REGENERATE]", fontFamily = com.cryptodept.ui.theme.JetBrainsMono)
                }
            }
            Text(
                text = briefing.date,
                color = Color(0xFF00FF41).copy(alpha = 0.7f),
                fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            HorizontalDivider(color = Color(0xFF00FF41), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
        }

        item {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = "MARKET SUMMARY:",
                    color = Color(0xFFFFB000),
                    fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = briefing.marketSentence,
                    color = Color.White,
                    fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            HorizontalDivider(color = Color(0xFF00FF41), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
        }

        item {
            Text(
                text = "KEY METRICS:",
                color = Color(0xFF00FF41),
                fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(briefing.keyMetrics) { metric ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = metric.label.padEnd(15),
                    color = Color.Gray,
                    fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                    fontSize = 12.sp
                )
                Text(
                    text = metric.value,
                    color = Color.White,
                    fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                    fontSize = 12.sp
                )
                Text(
                    text = metric.change ?: "",
                    color = if (metric.sentiment == "BULLISH") Color(0xFF00FF41) else Color(0xFFFF3B30),
                    fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                    fontSize = 12.sp
                )
                Text(
                    text = metric.sentiment,
                    color = when(metric.sentiment) {
                        "BULLISH" -> Color(0xFF00FF41)
                        "BEARISH" -> Color(0xFFFF3B30)
                        else -> Color(0xFFFFB000)
                    },
                    fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                    fontSize = 10.sp
                )
            }
        }

        item {
            HorizontalDivider(color = Color(0xFF00FF41), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = "⚠ ALERTS (${briefing.topAlerts.size}):",
                color = Color(0xFFFFB000),
                fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(briefing.topAlerts) { alert ->
            val color = when(alert.severity) {
                DailyBriefingGenerator.AlertSeverity.CRITICAL -> Color(0xFFFF3B30)
                DailyBriefingGenerator.AlertSeverity.WARNING -> Color(0xFFFFB000)
                DailyBriefingGenerator.AlertSeverity.INFO -> Color(0xFF00FF41)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .border(0.5.dp, color, RectangleShape)
                    .padding(8.dp)
            ) {
                Text(
                    text = "[${alert.severity.name}] ${alert.title}",
                    color = color,
                    fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text(
                    text = alert.detail,
                    color = color.copy(alpha = 0.8f),
                    fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                    fontSize = 11.sp
                )
            }
        }

        item {
            HorizontalDivider(color = Color(0xFF00FF41), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = "RECOMMENDATION:",
                color = Color(0xFF00FF41),
                fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Text(
                text = briefing.tradingSuggestion,
                color = Color.White,
                fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                fontSize = 14.sp,
                modifier = Modifier.padding(8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
