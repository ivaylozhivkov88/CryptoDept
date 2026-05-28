package com.cryptodept.ui.dashboard.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
fun MissionControlWidget(
    session: MarketSession,
    brief: String?,
    modifier: Modifier = Modifier
) {
    val colors = LocalTerminalColors.current
    
    // Only show for Morning or specific Active transitions
    val isMorning = session == MarketSession.MORNING || session == MarketSession.ASIAN
    val showBrief = !brief.isNullOrBlank()

    AnimatedVisibility(
        visible = showBrief,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .border(1.dp, colors.primary.copy(alpha = 0.4f), RectangleShape)
                .background(colors.primary.copy(alpha = 0.05f))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ">>> SESSION_MISSION_CONTROL",
                    color = colors.primary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "[${session.displayName.uppercase()}]",
                    color = colors.primary.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = brief ?: "SYNCHRONIZING_STRATEGIC_OBJECTIVES...",
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 16.sp
            )
        }
    }
}
