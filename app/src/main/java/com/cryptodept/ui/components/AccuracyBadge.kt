package com.cryptodept.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun AccuracyBadge(
    modelName: String,
    accuracy: Float,  // 0.0-1.0
    modifier: Modifier = Modifier
) {
    val accuracyPercent = (accuracy * 100).toInt()
    val badgeColor = when {
        accuracy >= 0.6f -> Color(0xFF00FF41)  // Green for good accuracy
        accuracy >= 0.4f -> Color(0xFFFFB000)  // Amber for moderate
        else -> Color(0xFFFF4444)                // Red for poor
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .border(1.dp, badgeColor)
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$modelName: $accuracyPercent%",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = badgeColor,
            maxLines = 1
        )
    }
}

