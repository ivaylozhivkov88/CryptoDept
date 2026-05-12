package com.cryptodept.ui.boot

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.ui.theme.LocalTerminalAudioManager
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.util.TerminalAudioService
import kotlinx.coroutines.delay

@Composable
fun BootSequenceScreen(onBootComplete: () -> Unit) {
    val colors = LocalTerminalColors.current
    val soundService = LocalTerminalAudioManager.current
    val displayedLines = remember { mutableStateListOf<String>() }

    val fullLogs =
        listOf(
            "CRYPTODEPT TERMINAL v6.0",
            "========================",
            "(c) 2026 WORLD #1 ELITE MODE",
            "",
            "SYSTEM BOOT SEQUENCE INITIATED...",
            "",
            "[OK] MEMORY CHECK.............. 2048MB",
            "[OK] STORAGE DRIVER............ ROOM DB v11",
            "[OK] NETWORK INTERFACE......... MULTI-API",
            "[OK] WEBSOCKET DAEMON.......... BINANCE FEED",
            "[OK] PREDICTION ENGINE......... ENSEMBLE v3",
            "[OK] AGENTIC OVERSIGHT......... ACTIVE",
            "[OK] RISK CALCULATOR........... ONLINE",
            "[OK] FIREBASE SERVICES......... CONNECTED",
            "[..] LOADING MARKET DATA.......",
            "",
            "BOOT COMPLETE. ENTERING TERMINAL.",
        )

    LaunchedEffect(Unit) {
        fullLogs.forEach { line ->
            if (line.isEmpty()) {
                displayedLines.add("")
                delay(20)
            } else {
                var currentText = ""
                displayedLines.add("")
                line.forEach { char ->
                    currentText += char
                    displayedLines[displayedLines.size - 1] = currentText
                    // No character delay, or very small
                }
                if (line.startsWith("[OK]")) {
                    soundService?.playSound(TerminalAudioService.SOUND_CLICK)
                }
                delay(20) // Fast line delay
            }
        }
        delay(100)
        soundService?.playSound(TerminalAudioService.SOUND_BOOT)
        onBootComplete()
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(24.dp),
    ) {
        Column {
            displayedLines.forEachIndexed { index, line ->
                Row {
                    Text(
                        text = line,
                        color = colors.primary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 1.dp),
                    )
                    if (index == displayedLines.size - 1) {
                        BlinkingCursor()
                    }
                }
            }
        }
    }
}

@Composable
fun BlinkingCursor() {
    val colors = LocalTerminalColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "cursor_alpha",
    )

    Box(
        modifier =
            Modifier
                .size(10.dp, 16.dp)
                .background(colors.primary.copy(alpha = alpha)),
    )
}
