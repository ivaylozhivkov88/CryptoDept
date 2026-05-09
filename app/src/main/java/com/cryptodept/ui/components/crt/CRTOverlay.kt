package com.cryptodept.ui.components.crt

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.cryptodept.ui.theme.LocalTerminalColors

@Composable
fun CRTOverlay(modifier: Modifier = Modifier) {
    val colors = LocalTerminalColors.current
    if (!colors.isPhosphor) return // White mode — no overlay

    val infiniteTransition = rememberInfiniteTransition(label = "crt_fx")

    val flickerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(100, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "flicker",
    )

    val scanlineOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "scanline_move",
    )

    Spacer(
        modifier =
            modifier
                .fillMaxSize()
                .graphicsLayer(alpha = flickerAlpha)
                .drawWithCache {
                    val scanlineSpacing = 3.dp.toPx()
                    val lineCount = (size.height / scanlineSpacing).toInt()
                    val phosphorColor = colors.primary.copy(alpha = 0.03f)

                    onDrawWithContent {
                        drawContent()

                        // 1. Static Scanlines
                        repeat(lineCount) { i ->
                            drawLine(
                                Color.Black.copy(alpha = 0.15f),
                                Offset(0f, i * scanlineSpacing),
                                Offset(size.width, i * scanlineSpacing),
                                1f,
                            )
                        }

                        // 2. Moving Interference Line
                        val movingY = scanlineOffset * size.height
                        drawRect(
                            brush =
                                Brush.verticalGradient(
                                    0f to Color.Transparent,
                                    0.45f to Color.Transparent,
                                    0.5f to Color.White.copy(alpha = 0.05f),
                                    0.55f to Color.Transparent,
                                    1f to Color.Transparent,
                                ),
                            topLeft = Offset(0f, movingY - 50.dp.toPx()),
                            size = size.copy(height = 100.dp.toPx()),
                        )

                        // 3. Phosphor Glow (Persistence simulation)
                        drawRect(color = phosphorColor)

                        // 4. Vignette (Rounded screen effect)
                        drawRect(
                            Brush.radialGradient(
                                0.0f to Color.Transparent,
                                0.6f to Color.Transparent,
                                1.0f to Color.Black.copy(0.45f),
                                center = center,
                                radius = size.maxDimension / 1.1f,
                            ),
                        )
                    }
                },
    )
}
