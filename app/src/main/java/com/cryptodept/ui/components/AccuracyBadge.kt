package com.cryptodept.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.ui.theme.LocalTerminalColors

/**
 * Honest accuracy display for AI predictions.
 * 
 * Design philosophy:
 *   - SHOW TRUTH: Even if accuracy is below 50%, show it.
 *   - INSUFFICIENT DATA HANDLING: If < 10 verified predictions, say so.
 *   - COIN-FLIP COMPARISON: Make it obvious if model is better than chance.
 */
@Composable
fun AccuracyBadge(
    accuracyPercent: Int?,
    sampleSize: Int?,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    coinId: String = "BTC",
    timeframe: String = "24h"
) {
    val colors = LocalTerminalColors.current
    
    if (accuracyPercent == null || sampleSize == null || sampleSize < 10) {
        // Insufficient data — honest message
        Surface(
            color = colors.dimText.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, colors.dimText),
            shape = RectangleShape,
            modifier = modifier,
        ) {
            Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text(
                    text = if (compact) "⚠ NEW" else "⚠ INSUFFICIENT DATA",
                    color = colors.dimText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = if (compact) 11.sp else 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Accuracy tracking in progress",
                    color = colors.dimText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        return
    }
    
    val accuracyColor = when {
        accuracyPercent >= 60 -> colors.primary
        accuracyPercent >= 50 -> colors.amber
        accuracyPercent >= 40 -> colors.amber
        else -> colors.danger
    }
    
    Surface(
        color = accuracyColor.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, accuracyColor),
        shape = RectangleShape,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
            Text(
                text = if (compact) "$accuracyPercent% (n=$sampleSize)" else "$accuracyPercent% accuracy",
                color = accuracyColor,
                fontFamily = FontFamily.Monospace,
                fontSize = if (compact) 11.sp else 13.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Historically correct on ${coinId.uppercase()} $timeframe calls",
                color = colors.dimText,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
