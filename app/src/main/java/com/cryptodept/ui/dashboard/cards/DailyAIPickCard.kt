package com.cryptodept.ui.dashboard.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.ui.components.AccuracyBadge
import com.cryptodept.ui.components.FeatureHelpIcon
import com.cryptodept.domain.tier.FeatureKey
import com.cryptodept.ui.theme.LocalTerminalColors

/**
 * Daily AI Pick card — Free tier feature.
 * One coin prediction per day, deterministic rotation.
 */
@Composable
fun DailyAIPickCard(
    coinSymbol: String,
    direction: String,
    confidencePercent: Int,
    accuracyPercent: Int?,
    sampleSize: Int?,
    onSeeAllPredictions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTerminalColors.current
    
    Card(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = colors.background),
        border = BorderStroke(1.dp, colors.amber),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = ">>> TODAYS_AI_PICK",
                        color = colors.amber,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                    FeatureHelpIcon(feature = FeatureKey.DAILY_AI_PICK, iconSize = 12.dp)
                }
                Text(
                    text = "[1_PER_DAY]",
                    color = colors.dimText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Coin + Direction
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = coinSymbol,
                    color = colors.primary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = when (direction.uppercase()) {
                        "UP", "BULLISH", "STRONG_UP" -> "🟢 BULLISH"
                        "DOWN", "BEARISH", "STRONG_DOWN" -> "🔴 BEARISH"
                        else -> "⚪ NEUTRAL"
                    },
                    color = colors.textPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Confidence
            Text(
                text = "Confidence: $confidencePercent%",
                color = colors.textPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            // Honest accuracy
            AccuracyBadge(
                accuracyPercent = accuracyPercent,
                sampleSize = sampleSize,
                compact = true,
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Not financial advice. DYOR.",
                color = colors.dimText,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            TextButton(
                onClick = onSeeAllPredictions,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(
                    text = "[ All_predictions_→ ]",
                    color = colors.amber,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                )
            }
        }
    }
}
