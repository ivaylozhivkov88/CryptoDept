package com.cryptodept.ui.tutorial

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.cryptodept.ui.theme.LocalTerminalColors

/**
 * Full-screen overlay that:
 * 1. Dims everything to ~80% opacity black
 * 2. Cuts out a rectangular spotlight around the target Rect
 * 3. Draws a pulsing phosphor border around the spotlight
 */
@Composable
fun TutorialSpotlight(
    targetBounds: Rect?,
    cornerRadius: Float = 16f,
    padding: Float = 12f
) {
    val colors = LocalTerminalColors.current

    // Animate target rect transitions smoothly
    val animatedBounds = animateRectAsState(
        targetValue = targetBounds ?: Rect.Zero,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "spotlight_bounds"
    )

    // Pulsing glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (targetBounds == null) {
            // Fully dim screen when no target
            drawRect(Color.Black.copy(alpha = 0.95f))
            return@Canvas
        }

        val bounds = animatedBounds.value
        val paddedRect = Rect(
            left = bounds.left - padding,
            top = bounds.top - padding,
            right = bounds.right + padding,
            bottom = bounds.bottom + padding
        )

        // Step 1: Draw dim everywhere
        drawRect(Color.Black.copy(alpha = 0.95f))

        // Step 2: Cut out spotlight (BlendMode.Clear)
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(paddedRect.left, paddedRect.top),
            size = Size(paddedRect.width, paddedRect.height),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            blendMode = BlendMode.Clear
        )

        // Step 3: Draw glowing border around spotlight
        drawRoundRect(
            color = colors.primary.copy(alpha = glowAlpha),
            topLeft = Offset(paddedRect.left, paddedRect.top),
            size = Size(paddedRect.width, paddedRect.height),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            style = Stroke(width = 3f)
        )

        // Step 4: Outer glow halo
        drawRoundRect(
            color = colors.primary.copy(alpha = glowAlpha * 0.3f),
            topLeft = Offset(paddedRect.left - 4f, paddedRect.top - 4f),
            size = Size(paddedRect.width + 8f, paddedRect.height + 8f),
            cornerRadius = CornerRadius(cornerRadius + 2f, cornerRadius + 2f),
            style = Stroke(width = 8f)
        )
    }
}

@Composable
private fun animateRectAsState(
    targetValue: Rect,
    animationSpec: AnimationSpec<Float> = spring(),
    label: String = ""
): State<Rect> {
    val left = animateFloatAsState(targetValue.left, animationSpec, label = "$label.left")
    val top = animateFloatAsState(targetValue.top, animationSpec, label = "$label.top")
    val right = animateFloatAsState(targetValue.right, animationSpec, label = "$label.right")
    val bottom = animateFloatAsState(targetValue.bottom, animationSpec, label = "$label.bottom")

    return derivedStateOf {
        Rect(left.value, top.value, right.value, bottom.value)
    }
}
