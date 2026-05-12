package com.cryptodept.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.cryptodept.ui.theme.LocalTerminalColors

/**
 * Displays text with a blinking CRT cursor at the end.
 * When isStreaming is true → shows blinking █ cursor.
 * When isStreaming is false → cursor disappears.
 *
 * Usage:
 *   StreamingText(
 *       text = uiState.aiNarrativeText,
 *       isStreaming = uiState.isAiNarrativeStreaming,
 *       modifier = Modifier.fillMaxWidth()
 *   )
 */
@Composable
fun StreamingText(
    text: String,
    isStreaming: Boolean,
    modifier: Modifier = Modifier,
    textColor: Color? = null,
    cursorColor: Color? = null,
    fontSize: TextUnit = 14.sp,
    placeholder: String = "AWAITING_TRANSMISSION..."
) {
    val colors = LocalTerminalColors.current
    val resolvedTextColor = textColor ?: colors.textPrimary
    val resolvedCursorColor = cursorColor ?: colors.primary

    // Blinking cursor animation
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                0f at 0
                1f at 200
                1f at 500
                0f at 700
                0f at 1000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "cursor_alpha"
    )

    val displayText: AnnotatedString = if (text.isEmpty() && !isStreaming) {
        // Empty + not loading = placeholder
        buildAnnotatedString {
            withStyle(SpanStyle(color = colors.dimText)) {
                append(placeholder)
            }
        }
    } else {
        buildAnnotatedString {
            withStyle(SpanStyle(color = resolvedTextColor)) {
                append(text)
            }
            if (isStreaming) {
                // Append blinking cursor at end
                withStyle(SpanStyle(
                    color = resolvedCursorColor.copy(alpha = cursorAlpha),
                    fontWeight = FontWeight.Bold
                )) {
                    append("█")
                }
            }
        }
    }

    Text(
        text = displayText,
        modifier = modifier,
        fontSize = fontSize,
        fontFamily = FontFamily.Monospace,
        lineHeight = (fontSize.value * 1.4f).sp
    )
}
