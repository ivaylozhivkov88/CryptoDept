package com.cryptodept.ui.dashboard.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.util.MarketSession

@Composable
fun MissionControlCard(
    session: MarketSession,
    sessionBrief: String?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTerminalColors.current

    if (sessionBrief == null) return // invisible when no data

    val sessionColor = when (session) {
        MarketSession.MORNING   -> colors.primary
        MarketSession.ACTIVE    -> colors.amber
        MarketSession.NY_OPEN   -> colors.danger
        MarketSession.EVENING   -> colors.dimText
        else                    -> colors.dimText
    }

    val blinkAlpha by rememberInfiniteTransition(label = "session_blink")
        .animateFloat(
            initialValue = 1f,
            targetValue = if (session == MarketSession.NY_OPEN) 0.3f else 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "blink",
        )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .border(1.dp, sessionColor.copy(alpha = 0.5f), RectangleShape)
            .background(sessionColor.copy(alpha = 0.04f))
            .padding(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = ">>> ${session.displayName.uppercase()}",
                color = sessionColor.copy(alpha = blinkAlpha),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
            )
            Canvas(modifier = Modifier.size(6.dp)) {
                drawCircle(color = sessionColor.copy(alpha = blinkAlpha))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = sessionBrief,
            color = colors.textPrimary,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            lineHeight = 16.sp,
        )
    }
}
