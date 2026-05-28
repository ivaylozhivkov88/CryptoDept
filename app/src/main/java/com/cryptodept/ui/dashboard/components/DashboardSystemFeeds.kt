package com.cryptodept.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.domain.model.AgentStatus
import com.cryptodept.ui.theme.LocalTerminalColors
import java.util.Locale

@Composable
fun AgentStatusLine(agentStatuses: Map<String, AgentStatus>) {
    val colors = LocalTerminalColors.current
    val allActive = agentStatuses.values.all { it == AgentStatus.SUCCESS }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(if (allActive) colors.primary else colors.amber, CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (allActive) "ALL_INTELLIGENCE_NODES_ACTIVE" else "SYSTEM_SYNCHRONIZING...",
            color = if (allActive) colors.primary.copy(0.6f) else colors.amber.copy(0.6f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun SystemIntegrityFeed(logs: List<com.cryptodept.domain.manager.IntegrityLog>) {
    val colors = LocalTerminalColors.current
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp)
            .border(0.5.dp, colors.grid.copy(alpha = 0.3f), RectangleShape)
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { isExpanded = !isExpanded }
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ">>> INTEGRITY_WATCHDOG_LOGS",
                color = colors.dimText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isExpanded) "[-]" else "[+]",
                color = colors.primary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        if (isExpanded) {
            Spacer(modifier = Modifier.height(4.dp))
            if (logs.isEmpty()) {
                Text(
                    text = "WAITING FOR AGENT-INTEGRITY SCAN...",
                    color = colors.grid,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                logs.takeLast(10).forEach { log ->
                    val timeStr = java.text.SimpleDateFormat("HH:mm:ss", Locale.US).format(java.util.Date(log.timestamp))
                    Text(
                        text = "[$timeStr] ${log.message}",
                        color = if (log.isAnomaly) colors.danger else colors.primary.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
