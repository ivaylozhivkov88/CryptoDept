package com.cryptodept.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.ui.theme.LocalTerminalColors
import kotlinx.coroutines.delay

@Composable
fun PsychologyLockOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit = {}, // Not used as per new instructions
) {
    val colors = LocalTerminalColors.current
    val hapticManager = com.cryptodept.ui.components.LocalHapticManager.current
    
    LaunchedEffect(isVisible) {
        if (isVisible) {
            hapticManager?.tiltLock()
        }
    }

    val blinkAlpha by rememberInfiniteTransition(label = "blink").animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blink_alpha",
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.98f))
                    .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.danger, RectangleShape)
                        .background(colors.danger.copy(alpha = 0.05f))
                        .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 1. Blinking Header
                Text(
                    text = "[TILT_DETECTED]",
                    color = colors.danger,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.alpha(blinkAlpha)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Subtitle
                Text(
                    text = "Emotional volatility detected. Terminal locked for your protection.",
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 3. Countdown (Simulated for now as per instructions)
                var countdownSeconds by remember { mutableIntStateOf(900) } // 15 mins
                LaunchedEffect(isVisible) {
                    if (isVisible) {
                        while (countdownSeconds > 0) {
                            delay(1000)
                            countdownSeconds--
                        }
                    }
                }

                val minutes = countdownSeconds / 60
                val seconds = countdownSeconds % 60
                Text(
                    text = "Auto-unlock in: ${String.format("%02d:%02d", minutes, seconds)}",
                    color = colors.amber,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 4. Footer
                Text(
                    text = "This is Tilt Protection — a feature designed to prevent emotional trading decisions.",
                    color = colors.dimText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
