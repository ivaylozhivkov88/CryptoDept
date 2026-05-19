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
            Text(
                text = if (compact) {
                    "⚠ NEW"
                } else {
                    "⚠ INSUFFICIENT DATA — Track record builds with 10+ verified predictions"
                },
                color = colors.dimText,
                fontFamily = FontFamily.Monospace,
                fontSize = if (compact) 9.sp else 10.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        return
    }
    
    val accuracyColor = when {
        accuracyPercent >= 60 -> colors.primary
        accuracyPercent >= 50 -> colors.amber
        accuracyPercent >= 40 -> colors.amber
        else -> colors.danger
    }
    
    val betterThanCoinFlip = accuracyPercent > 50
    
    Surface(
        color = accuracyColor.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, accuracyColor),
        shape = RectangleShape,
        modifier = modifier,
    ) {
        if (compact) {
            Text(
                text = "$accuracyPercent% (n=$sampleSize)",
                color = accuracyColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        } else {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = ">>> TRACK_RECORD",
                    color = colors.amber,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "$accuracyPercent% accuracy",
                    color = accuracyColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                
                Text(
                    text = "Based on $sampleSize verified predictions",
                    color = colors.textPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = if (betterThanCoinFlip) {
                        "✓ Better than coin flip (>50%)"
                    } else {
                        "⚠ Below coin flip rate — use with extreme caution"
                    },
                    color = if (betterThanCoinFlip) colors.primary else colors.danger,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Past performance ≠ future results.",
                    color = colors.dimText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                )
            }
        }
    }
}
