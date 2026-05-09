package com.cryptodept.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.ui.theme.LocalTerminalColors

@Composable
fun SentimentBadge(
    pulse: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTerminalColors.current
    val badgeColor =
        when {
            pulse >= 70 -> colors.primary
            pulse >= 55 -> colors.primary.copy(alpha = 0.7f)
            pulse >= 45 -> colors.amber
            pulse >= 30 -> colors.danger.copy(alpha = 0.7f)
            else -> colors.danger
        }

    Box(
        modifier =
            modifier
                .background(colors.background)
                .border(1.dp, badgeColor, RectangleShape)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "SENTIMENT: $label [$pulse/100]",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = badgeColor,
        )
    }
}
