package com.cryptodept.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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

@Composable
fun ErrorState(
    modifier: Modifier = Modifier,
    message: String = "SYSTEM_FAILURE: DATA_STREAM_INTERRUPTED",
    onRetry: () -> Unit,
) {
    val colors = LocalTerminalColors.current

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = colors.danger,
            modifier = Modifier.size(64.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "![ERROR]!",
            color = colors.danger,
            fontFamily = FontFamily.Monospace,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = message,
            color = colors.danger,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .border(1.dp, colors.danger)
                    .padding(8.dp),
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRetry,
            shape = RectangleShape,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = colors.danger,
                    contentColor = Color.Black,
                ),
        ) {
            Text(text = "REBOOT_CONNECTION", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }
    }
}
