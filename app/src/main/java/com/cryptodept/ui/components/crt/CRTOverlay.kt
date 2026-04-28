package com.cryptodept.ui.components.crt

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * CRT Overlay component for CryptoDept v2.
 * Includes: Scanlines, Vignette (Screen Curvature), and Subtle Flicker.
 */
@Composable
fun CRTOverlay(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "CRT_Effects")

    // Effect 4: Subtle Flicker (0.97f to 1.0f)
    val flickerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Flicker"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(alpha = flickerAlpha)
    ) {
        val width = size.width
        val height = size.height

        // Effect 1: Scanlines (4dp spacing, 0.08f opacity)
        val scanlineSpacing = 4.dp.toPx()
        val lineCount = (height / scanlineSpacing).toInt()
        
        for (i in 0..lineCount) {
            val y = i * scanlineSpacing
            drawLine(
                color = Color.Black.copy(alpha = 0.12f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Effect 2: Screen Curvature (Vignette)
        // Radial gradient from transparent center to Black(0.4f) corners
        drawRect(
            brush = Brush.radialGradient(
                0.0f to Color.Transparent,
                0.7f to Color.Transparent,
                1.3f to Color.Black.copy(alpha = 0.4f),
                center = center,
                radius = size.maxDimension / 1.2f
            ),
            blendMode = BlendMode.Multiply
        )
    }
}
