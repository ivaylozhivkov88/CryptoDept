package com.cryptodept.ui.agents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.cryptodept.data.db.IntelligenceBriefingEntity
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.util.TerminalConfig
import com.cryptodept.viewmodel.AgentHubViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AgentHubScreen(
    navController: NavController,
    viewModel: AgentHubViewModel = hiltViewModel()
) {
    val briefings by viewModel.briefings.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val colors = LocalTerminalColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ">>> AGENTIC_INTELLIGENCE_HUB",
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = colors.primary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "[REFRESH]",
                    color = colors.primary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.clickable { viewModel.refresh() }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))

        HorizontalDivider(color = colors.grid, thickness = 1.dp)

        if (briefings.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NO_ANOMALIES_DETECTED_IN_PREVIOUS_CYCLES",
                    color = colors.dimText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(briefings) { briefing ->
                    BriefingCard(briefing)
                }
            }
        }
    }
}

@Composable
fun BriefingCard(briefing: IntelligenceBriefingEntity) {
    val colors = LocalTerminalColors.current
    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(briefing.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.primary, RectangleShape),
        colors = CardDefaults.cardColors(containerColor = colors.background.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "[$dateStr]",
                    color = colors.dimText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "ANOMALY_SCORE: ${briefing.anomalyScore}",
                    color = if (briefing.anomalyScore > 80) colors.danger else colors.primary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = briefing.summary,
                color = colors.textPrimary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = ">>> EVIDENCE_LOG:",
                color = colors.amber,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            briefing.evidence.split(",").forEach { detail ->
                Text(
                    text = " - $detail",
                    color = colors.dimText,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Badge(label = "SENTIMENT: ${briefing.sentiment}")
                Spacer(modifier = Modifier.width(8.dp))
                Badge(label = "RISK_LEVEL: ${briefing.riskScore}/100")
            }
        }
    }
}

@Composable
fun Badge(label: String) {
    val colors = LocalTerminalColors.current
    Surface(
        color = colors.primary.copy(alpha = 0.1f),
        border = borderStroke(0.5.dp, colors.primary),
        shape = RectangleShape
    ) {
        Text(
            text = label,
            color = colors.primary,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

private fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) = 
    androidx.compose.foundation.BorderStroke(width, color)
