package com.cryptodept.ui.effects

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import com.cryptodept.ui.theme.LocalSoundManager
import com.cryptodept.service.SoundManager
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Glitch effect for CryptoDept v2.
 * Simulates CRT signal interference with optimized stability.
 */
@Composable
fun GlitchEffect(
    trigger: Any?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val soundManager = LocalSoundManager.current
    var isGlitching by remember { mutableStateOf(false) }
    val random = remember { Random(System.currentTimeMillis()) }
    var params by remember { mutableStateOf(GlitchParams()) }

    LaunchedEffect(trigger) {
        if (trigger != null) {
            params = GlitchParams(
                offsetX = random.nextInt(-8, 8),
                offsetY = random.nextInt(-2, 2),
                alpha = random.nextFloat().coerceIn(0.85f, 1.0f),
                scale = random.nextFloat().coerceIn(0.99f, 1.01f)
            )
            isGlitching = true
            soundManager?.playSound(SoundManager.SOUND_GLITCH)
            delay(random.nextLong(60, 150))
            isGlitching = false
        }
    }

    val glitchOffset by animateIntOffsetAsState(
        targetValue = if (isGlitching) IntOffset(params.offsetX, params.offsetY) else IntOffset.Zero,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "glitchOffset"
    )

    val glitchAlpha by animateFloatAsState(
        targetValue = if (isGlitching) params.alpha else 1.0f,
        animationSpec = tween(durationMillis = 80),
        label = "glitchAlpha"
    )

    val glitchScale by animateFloatAsState(
        targetValue = if (isGlitching) params.scale else 1.0f,
        animationSpec = spring(),
        label = "glitchScale"
    )

    Box(
        modifier = modifier.graphicsLayer {
            translationX = glitchOffset.x.toFloat()
            translationY = glitchOffset.y.toFloat()
            alpha = glitchAlpha
            scaleX = glitchScale
            scaleY = glitchScale
        }
    ) {
        content()
    }
}

data class GlitchParams(
    val offsetX: Int = 0,
    val offsetY: Int = 0,
    val alpha: Float = 1f,
    val scale: Float = 1f
)
