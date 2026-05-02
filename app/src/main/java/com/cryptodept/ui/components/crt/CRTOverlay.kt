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
    if (!colors.isPhosphor) return  // White mode — без overlay
    
    val flickerAlpha by rememberInfiniteTransition(label = "flicker")
        .animateFloat(0.97f, 1.0f, infiniteRepeatable(tween(150), RepeatMode.Reverse), label = "f")
    
    Spacer(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(alpha = flickerAlpha)
            .drawWithCache {
                // Изчисли scanlines ВЕДНЪЖ
                val scanlineSpacing = 4.dp.toPx()
                val lineCount = (size.height / scanlineSpacing).toInt()
                
                onDrawWithContent {
                    drawContent()
                    
                    // Scanlines
                    repeat(lineCount) { i ->
                        drawLine(
                            Color.Black.copy(alpha = 0.10f), 
                            Offset(0f, i * scanlineSpacing), 
                            Offset(size.width, i * scanlineSpacing), 
                            1f
                        )
                    }
                    
                    // Vignette
                    drawRect(
                        Brush.radialGradient(
                            listOf(Color.Transparent, Color.Transparent, Color.Black.copy(0.35f)),
                            radius = size.maxDimension / 1.2f
                        )
                    )
                }
            }
    )
}
