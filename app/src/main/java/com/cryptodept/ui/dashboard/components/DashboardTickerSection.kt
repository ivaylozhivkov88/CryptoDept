package com.cryptodept.ui.dashboard.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.domain.model.CoinPrice
import com.cryptodept.domain.model.NetworkHealth
import com.cryptodept.ui.components.FreshnessIndicator
import com.cryptodept.ui.components.TickerTape
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.ui.tutorial.tutorialTarget
import com.cryptodept.domain.tutorial.TutorialTargetId

@Composable
fun DashboardTickerSection(
    currentPrices: List<CoinPrice>,
    networkHealth: NetworkHealth?,
    pricesLastUpdated: Long,
    isCloudLive: Boolean,
    showVerdict: Boolean = true
) {
    val colors = LocalTerminalColors.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "MARKET_TICKER",
                color = colors.dimText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            FreshnessIndicator(lastUpdatedMs = pricesLastUpdated, label = "PRICES")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "Alpha"
            )

            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(8.dp)
                    .alpha(if (isCloudLive) alpha else 1f)
                    .background(if (isCloudLive) colors.primary else colors.danger, CircleShape)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(35.dp)
                    .tutorialTarget(TutorialTargetId.DASH_PRICE_TICKER)
            ) {
                TickerTape(
                    prices = currentPrices,
                    networkHealth = networkHealth,
                    modifier = Modifier.fillMaxSize(),
                    speed = 1.0f
                )
            }
        }

        if (showVerdict) {
            GlobalVerdictStrip(networkHealth)
        }
    }
}

@Composable
fun GlobalVerdictStrip(networkHealth: NetworkHealth?) {
    val colors = LocalTerminalColors.current
    networkHealth?.let { health ->
        Spacer(modifier = Modifier.height(8.dp))
        val (verdict, color) = when {
            health.fearGreedIndex <= 25 -> "EXTREME_PANIC_ACCUMULATE" to colors.error
            health.fearGreedIndex <= 45 -> "FEARFUL_CONSOLIDATION" to colors.amber
            health.fearGreedIndex <= 55 -> "NEUTRAL_STASIS" to colors.textPrimary.copy(alpha = 0.7f)
            health.fearGreedIndex <= 75 -> "BULLISH_EXPANSION" to colors.primary
            else -> "EUPHORIC_DISTRIBUTION" to colors.primary
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, color.copy(alpha = 0.3f))
                .background(color.copy(alpha = 0.05f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ">>> GLOBAL_VERDICT:",
                color = color,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = verdict,
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black
            )
        }
    }
}
