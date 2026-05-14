package com.cryptodept.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.ui.theme.LocalTerminalColors

import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ErrorState(
    modifier: Modifier = Modifier,
    message: String = "SYSTEM_FAILURE: DATA_STREAM_INTERRUPTED",
    onRetry: () -> Unit,
) {
    val colors = LocalTerminalColors.current
    val scope = rememberCoroutineScope()
    var isRetrying by remember { mutableStateOf(false) }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (isRetrying) Icons.Default.Refresh else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isRetrying) colors.primary else colors.danger,
            modifier = Modifier.size(64.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (isRetrying) ">>> REBOOTING..." else "![ERROR]!",
            color = if (isRetrying) colors.primary else colors.danger,
            fontFamily = FontFamily.Monospace,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (isRetrying) "SYNCHRONIZING SYSTEM CLOCK..." else message,
            color = if (isRetrying) colors.primary else colors.danger,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .border(1.dp, if (isRetrying) colors.primary else colors.danger)
                    .padding(8.dp),
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (!isRetrying) {
                    isRetrying = true
                    scope.launch {
                        delay(800) // Visual confirmation
                        onRetry()
                        isRetrying = false
                    }
                }
            },
            enabled = !isRetrying,
            shape = RectangleShape,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = if (isRetrying) colors.primary else colors.danger,
                    contentColor = Color.Black,
                ),
        ) {
            Text(
                text = if (isRetrying) "WAITING..." else "REBOOT_CONNECTION", 
                fontFamily = FontFamily.Monospace, 
                fontWeight = FontWeight.Bold
            )
        }
    }
}
