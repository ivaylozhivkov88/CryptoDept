package com.cryptodept.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import kotlinx.coroutines.launch

enum class PriceDirection { UP, DOWN, NONE }

@Composable
fun rememberPriceFlash(price: Double): Pair<PriceDirection, Float> {
    var previousPrice by remember { mutableStateOf(price) }
    var direction by remember { mutableStateOf(PriceDirection.NONE) }
    val flashAlpha = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(price) {
        if (price == previousPrice || previousPrice == 0.0) {
            previousPrice = price
            return@LaunchedEffect
        }
        
        direction = if (price > previousPrice) PriceDirection.UP else PriceDirection.DOWN
        previousPrice = price
        
        scope.launch {
            flashAlpha.snapTo(1f)
            flashAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
            )
            direction = PriceDirection.NONE
        }
    }

    return direction to flashAlpha.value
}
