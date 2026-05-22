package com.cryptodept.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.cryptodept.ui.theme.LocalTerminalColors

@Composable
fun ScanLineOverlay(
    modifier: Modifier = Modifier,
    isScanning: Boolean,
    onScanComplete: () -> Unit = {},
) {
    val colors = LocalTerminalColors.current
    val offsetY = remember { Animatable(0f) }

    LaunchedEffect(isScanning) {
        if (isScanning) {
            offsetY.snapTo(0f)
            offsetY.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 120, easing = LinearEasing)
            )
            onScanComplete()
        }
    }

    if (isScanning || (offsetY.value < 1f && offsetY.value > 0.01f)) {
        Canvas(modifier = modifier.fillMaxSize()) {
            val y = offsetY.value * size.height
            if (y > 0 && y < size.height) {
                // Main scan line
                drawLine(
                    color = colors.primary.copy(alpha = 0.5f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.5.dp.toPx(),
                )
                // Glow above the line
                drawLine(
                    color = colors.primary.copy(alpha = 0.1f),
                    start = Offset(0f, y - 4.dp.toPx()),
                    end = Offset(size.width, y - 4.dp.toPx()),
                    strokeWidth = 6.dp.toPx(),
                )
            }
        }
    }
}
