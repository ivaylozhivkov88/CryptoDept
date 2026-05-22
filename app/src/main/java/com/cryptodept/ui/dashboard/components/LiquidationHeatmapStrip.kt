package com.cryptodept.ui.dashboard.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.domain.model.LiquidationSummary
import com.cryptodept.ui.theme.LocalTerminalColors
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun LiquidationHeatmapStrip(
    summary: LiquidationSummary?,
    modifier: Modifier = Modifier
) {
    val colors = LocalTerminalColors.current
    val longDominance by animateFloatAsState(
        targetValue = summary?.longDominance ?: 0.5f,
        animationSpec = tween(600),
        label = "longDominance"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(colors.background)
            .drawWithContent {
                drawContent()
                // Bottom border: 0.5dp, TerminalColors.primary.copy(alpha=0.12f)
                drawLine(
                    color = colors.primary.copy(alpha = 0.12f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 0.5.dp.toPx()
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT side (weight 1f)
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "◄",
                    color = colors.error.copy(alpha = 0.65f),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = summary?.totalLongLiquidity?.toCompactUsd() ?: "$0",
                    color = colors.error.copy(alpha = 0.65f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = summary?.nearestLongLevel?.let { if (it > 0) "@${it.toPriceLabel()}" else "@--" } ?: "@--",
                    color = Color.White.copy(alpha = 0.20f),
                    fontSize = 7.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // CENTER
            Column(
                modifier = Modifier.padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = summary?.currentPrice?.let { "$${String.format(Locale.US, "%,.0f", it)}" } ?: "$--",
                    color = colors.primary.copy(alpha = 0.55f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "LIQ LEVELS",
                    color = colors.primary.copy(alpha = 0.22f),
                    fontSize = 6.5.sp,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // RIGHT side (weight 1f)
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End)
            ) {
                Text(
                    text = summary?.nearestShortLevel?.let { if (it > 0) "@${it.toPriceLabel()}" else "@--" } ?: "@--",
                    color = Color.White.copy(alpha = 0.20f),
                    fontSize = 7.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = summary?.totalShortLiquidity?.toCompactUsd() ?: "$0",
                    color = colors.primary.copy(alpha = 0.60f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "►",
                    color = colors.primary.copy(alpha = 0.40f),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Progress bar (2dp height)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Color.White.copy(alpha = 0.04f))
        ) {
            Box(
                modifier = Modifier
                    .weight(longDominance.coerceIn(0.01f, 0.99f))
                    .fillMaxHeight()
                    .background(colors.error.copy(alpha = 0.35f))
            )
            Box(
                modifier = Modifier
                    .width(1.5.dp)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.15f))
            )
            Box(
                modifier = Modifier
                    .weight((1f - longDominance).coerceIn(0.01f, 0.99f))
                    .fillMaxHeight()
                    .background(colors.primary.copy(alpha = 0.28f))
            )
        }
    }
}

private fun Double.toCompactUsd(): String {
    return when {
        this >= 1_000_000_000.0 -> "$${String.format(Locale.US, "%.1f", this / 1_000_000_000.0)}B"
        this >= 1_000_000.0 -> "$${String.format(Locale.US, "%.1f", this / 1_000_000.0)}M"
        else -> "$${this.toLong()}"
    }
}

private fun Double.toPriceLabel(): String {
    return if (this >= 1000) {
        "$${(this / 1000).roundToInt()}K"
    } else {
        "$${String.format(Locale.US, "%.2f", this)}"
    }
}
