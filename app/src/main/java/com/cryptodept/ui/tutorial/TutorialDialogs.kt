package com.cryptodept.ui.tutorial

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.cryptodept.R
import com.cryptodept.ui.theme.LocalTerminalColors

@Composable
fun TutorialStartDialog(onStart: () -> Unit, onSkip: () -> Unit) {
    val colors = LocalTerminalColors.current
    Dialog(onDismissRequest = { /* no dismiss by tap outside */ }) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .border(2.dp, colors.primary, RectangleShape),
            shape = RectangleShape,
            colors = CardDefaults.cardColors(containerColor = colors.background)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.tutorial_start_question),
                    color = colors.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = colors.primary.copy(alpha = 0.3f))
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.tutorial_start_body),
                    color = colors.primary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.tutorial_start_no),
                            color = colors.dimText,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = onStart,
                        modifier = Modifier.height(44.dp).wrapContentWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.background
                        ),
                        shape = RectangleShape,
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.tutorial_start_yes),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TutorialSkipConfirmDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    val colors = LocalTerminalColors.current
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                ">>> ABORT_CONFIRMATION",
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        },
        text = {
            Text(
                stringResource(R.string.tutorial_skip_confirm),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = colors.textPrimary
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("[ABORT]", color = colors.danger, fontFamily = FontFamily.Monospace)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("[RESUME]", color = colors.primary, fontFamily = FontFamily.Monospace)
            }
        },
        containerColor = colors.background,
        shape = RectangleShape
    )
}

@Composable
fun TutorialCompletionDialog(onDismiss: () -> Unit) {
    val colors = LocalTerminalColors.current
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(2.dp, colors.primary, RectangleShape),
            shape = RectangleShape,
            colors = CardDefaults.cardColors(containerColor = colors.background)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "🏆 ${stringResource(R.string.tutorial_completed_title)}",
                    color = colors.primary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.tutorial_completed_message),
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.background
                    ),
                    shape = RectangleShape
                ) {
                    Text(
                        "[LAUNCH_TERMINAL]",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
