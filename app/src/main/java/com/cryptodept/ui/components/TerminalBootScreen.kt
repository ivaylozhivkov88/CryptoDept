package com.cryptodept.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.ui.theme.LocalTerminalColors
import kotlinx.coroutines.delay

@Composable
fun TerminalBootScreen(onComplete: () -> Unit) {
    val colors = LocalTerminalColors.current
    val bootLogs = listOf(
        "ESTABLISHING_ENCRYPTED_TUNNEL...",
        "SYNCING_INTELLIGENCE_NODES...",
        "RECOVERING_USER_PREFERENCES...",
        "AUTHENTICATING_ELITE_OPERATOR...",
        "TERMINAL_READY"
    )
    
    var visibleLogsCount by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        bootLogs.forEachIndexed { index, _ ->
            delay(500)
            visibleLogsCount = index + 1
        }
        delay(600)
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text(
                text = ">>> CRYPTODEPT_SYSTEM_BOOT_V1.5",
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            repeat(visibleLogsCount) { index ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = ">> ${bootLogs[index]}",
                        color = colors.primary.copy(alpha = 0.8f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (index < bootLogs.size - 1 || visibleLogsCount == bootLogs.size) {
                        Text(
                            text = if (index == bootLogs.size - 1) "[ READY ]" else "[ OK ]",
                            color = if (index == bootLogs.size - 1) colors.amber else colors.primary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
