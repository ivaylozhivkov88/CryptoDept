package com.cryptodept.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.ui.theme.LocalTerminalColors

@Composable
fun PriceText(
    price: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 24.sp,
    color: Color = LocalTerminalColors.current.primary
) {
    var oldPrice by remember { mutableStateOf(price) }
    var priceDirection by remember { mutableIntStateOf(0) } // 1 for up, -1 for down, 0 for same

    LaunchedEffect(price) {
        if (price != oldPrice) {
            val oldVal = oldPrice.replace(Regex("[^\\d.]"), "").toDoubleOrNull() ?: 0.0
            val newVal = price.replace(Regex("[^\\d.]"), "").toDoubleOrNull() ?: 0.0
            priceDirection = if (newVal > oldVal) 1 else if (newVal < oldVal) -1 else 0

            kotlinx.coroutines.delay(600)
            oldPrice = price
            priceDirection = 0
        }
    }

    val backgroundColor by animateColorAsState(
        targetValue = when (priceDirection) {
            1 -> Color(0xFF003300)
            -1 -> Color(0xFF330000)
            else -> Color.Transparent
        },
        animationSpec = tween(300),
        label = "bgFlash"
    )

    val glowRadius by animateDpAsState(
        targetValue = if (priceDirection != 0) 8.dp else 0.dp,
        animationSpec = tween(500),
        label = "glow"
    )

    Box(
        modifier = modifier
            .background(backgroundColor)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (glowRadius > 0.dp) {
            Text(
                text = price,
                color = color.copy(alpha = 0.5f),
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.blur(glowRadius)
            )
        }

        AnimatedContent(
            targetState = price,
            transitionSpec = {
                if (priceDirection == 1) {
                    (slideInVertically { height -> height } + fadeIn(tween(150)))
                        .togetherWith(slideOutVertically { height -> -height } + fadeOut(tween(150)))
                } else if (priceDirection == -1) {
                    (slideInVertically { height -> -height } + fadeIn(tween(150)))
                        .togetherWith(slideOutVertically { height -> height } + fadeOut(tween(150)))
                } else {
                    fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                }
            },
            label = "priceAnimation"
        ) { targetPrice ->
            Text(
                text = targetPrice,
                color = color,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold
            )
        }
    }
}