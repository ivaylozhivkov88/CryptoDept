package com.cryptodept.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.ui.theme.TerminalColorSet

/**
 * Sanity check dialog — warns about risky decisions but does NOT block.
 */
@Composable
fun SanityCheckDialog(
    title: String,
    message: String,
    severity: SanitySeverity = SanitySeverity.WARNING,
    confirmLabel: String = "I_UNDERSTAND_THE_RISK",
    dismissLabel: String = "CANCEL",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalTerminalColors.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = severity.emoji, fontSize = 22.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = severity.color(colors),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
        },
        text = {
            Column {
                Text(
                    text = message,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = colors.textPrimary,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "[ $confirmLabel ]",
                    color = severity.color(colors),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "[ $dismissLabel ]",
                    color = colors.primary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                )
            }
        },
        containerColor = colors.background,
        titleContentColor = severity.color(colors),
        textContentColor = colors.textPrimary,
    )
}

enum class SanitySeverity(val emoji: String) {
    INFO("ℹ️"),
    WARNING("⚠️"),
    CRITICAL("🚨");
    
    fun color(colors: TerminalColorSet) = when (this) {
        INFO -> colors.primary
        WARNING -> colors.amber
        CRITICAL -> colors.danger
    }
}
