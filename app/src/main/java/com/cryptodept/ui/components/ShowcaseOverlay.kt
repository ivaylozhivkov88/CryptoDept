package com.cryptodept.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.cryptodept.ui.theme.LocalTerminalColors
import kotlinx.coroutines.delay

@Composable
fun ShowcaseOverlay(
    targetCoordinates: Rect?,
    text: String,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    isLastStep: Boolean = false
) {
    val colors = LocalTerminalColors.current

    // Background scrim with a hole
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {} // Block clicks to underlying UI
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (targetCoordinates != null) {
                val spotlightPath = Path().apply {
                    addRect(targetCoordinates.inflate(4.dp.toPx()))
                }
                clipPath(spotlightPath, clipOp = ClipOp.Difference) {
                    drawRect(Color.Black.copy(alpha = 0.8f))
                }
                
                // Draw border around the hole
                drawRect(
                    color = colors.primary,
                    topLeft = Offset(targetCoordinates.left - 4.dp.toPx(), targetCoordinates.top - 4.dp.toPx()),
                    size = Size(targetCoordinates.width + 8.dp.toPx(), targetCoordinates.height + 8.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                )
            } else {
                drawRect(Color.Black.copy(alpha = 0.85f))
            }
        }

        // Explanation text box
        Column(
            modifier = Modifier
                .align(if (targetCoordinates == null || targetCoordinates.top > 400.dp.value) Alignment.TopCenter else Alignment.BottomCenter)
                .padding(if (targetCoordinates == null) 64.dp else 32.dp)
                .fillMaxWidth()
                .background(Color.Black)
                .border(1.dp, colors.primary)
                .padding(24.dp)
        ) {
            TypewriterText(
                text = text,
                color = colors.primary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onSkip) {
                    Text("SKIP_TUTORIAL", color = colors.dimText, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = onNext,
                    modifier = Modifier.border(1.dp, colors.primary)
                ) {
                    Text(if (isLastStep) "FINISH" else "NEXT_COMMAND >", color = colors.primary, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun TypewriterText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    var displayedText by remember { mutableStateOf("") }
    
    LaunchedEffect(text) {
        displayedText = ""
        text.forEach { char ->
            displayedText += char
            delay(20)
        }
    }

    Text(
        text = ">>> $displayedText",
        color = color,
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
        modifier = modifier
    )
}

// Helper modifier to capture coordinates
fun Modifier.onTargetPositioned(onRect: (Rect) -> Unit): Modifier = this.onGloballyPositioned { coordinates ->
    val position = coordinates.positionInRoot()
    val size = coordinates.size
    onRect(Rect(position.x, position.y, position.x + size.width, position.y + size.height))
}
