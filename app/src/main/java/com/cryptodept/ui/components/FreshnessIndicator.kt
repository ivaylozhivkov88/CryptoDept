package com.cryptodept.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.ui.theme.LocalTerminalColors
import kotlinx.coroutines.delay

enum class DataFreshness {
    LIVE,       // < 30 seconds
    FRESH,      // 30s – 5 minutes
    STALE,      // 5 – 30 minutes
    OLD,        // > 30 minutes
}

fun Long.toFreshness(): DataFreshness {
    if (this <= 0) return DataFreshness.OLD
    val ageSeconds = (System.currentTimeMillis() - this) / 1000
    return when {
        ageSeconds < 30    -> DataFreshness.LIVE
        ageSeconds < 300   -> DataFreshness.FRESH
        ageSeconds < 1800  -> DataFreshness.STALE
        else               -> DataFreshness.OLD
    }
}

fun Long.toAgeString(): String {
    if (this <= 0) return "--"
    val ageSeconds = (System.currentTimeMillis() - this) / 1000
    return when {
        ageSeconds < 30   -> "LIVE"
        ageSeconds < 60   -> "${ageSeconds}s ago"
        ageSeconds < 3600 -> "${ageSeconds / 60} min ago"
        else              -> "${ageSeconds / 3600}h ago"
    }
}

@Composable
fun FreshnessIndicator(
    lastUpdatedMs: Long,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTerminalColors.current
    var ageString by remember(lastUpdatedMs) { mutableStateOf(lastUpdatedMs.toAgeString()) }
    val freshness = lastUpdatedMs.toFreshness()

    // Update every 30 seconds
    LaunchedEffect(lastUpdatedMs) {
        while (true) {
            ageString = lastUpdatedMs.toAgeString()
            delay(30_000L)
        }
    }

    val dotColor = when (freshness) {
        DataFreshness.LIVE  -> colors.primary
        DataFreshness.FRESH -> colors.primary.copy(alpha = 0.7f)
        DataFreshness.STALE -> colors.amber
        DataFreshness.OLD   -> colors.danger
    }

    val blinkAlpha by rememberInfiniteTransition(label = "blink").animateFloat(
        initialValue = 1f,
        targetValue = if (freshness == DataFreshness.LIVE) 0.3f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "dot_blink",
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = label,
            color = colors.dimText,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
        )
        Canvas(modifier = Modifier.size(6.dp)) {
            drawCircle(color = dotColor.copy(alpha = blinkAlpha), style = Fill)
        }
        Text(
            text = ageString,
            color = dotColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = if (freshness == DataFreshness.LIVE) FontWeight.Bold else FontWeight.Normal,
        )
    }
}
