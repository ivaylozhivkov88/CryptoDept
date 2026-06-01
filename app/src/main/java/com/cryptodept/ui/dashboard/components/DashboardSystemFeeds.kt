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
fun AgentStatusLine(
    statuses: Map<String, AgentStatus>,
    modifier: Modifier = Modifier
) {
    val colors = LocalTerminalColors.current
    val allActive = statuses.values.all { it == AgentStatus.SUCCESS }
    
    Row(
        modifier = modifier
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
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp)
            .border(0.5.dp, colors.grid.copy(alpha = 0.3f), RectangleShape)
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { 
                val lastLog = logs.firstOrNull()?.message ?: "AGENT-INTEGRITY: SCANNING_FOR_ANOMALIES..."
                android.widget.Toast.makeText(context, lastLog, android.widget.Toast.LENGTH_LONG).show()
            }
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
                text = "[ VIEW ]",
                color = colors.primary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
