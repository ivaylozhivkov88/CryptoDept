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
import com.cryptodept.ui.components.FeatureHelpIcon
import com.cryptodept.domain.tier.FeatureKey
import com.cryptodept.domain.model.WhaleSignal
import com.cryptodept.ui.theme.LocalTerminalColors

/**
 * Whale Insight card — Free tier version.
 * Shows processed signal instead of raw transactions.
 */
@Composable
fun WhaleInsightCard(
    signal: WhaleSignal,
    onLearnMore: () -> Unit,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTerminalColors.current
    
    Card(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = colors.background),
        border = BorderStroke(1.dp, colors.primary),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = ">>> WHALE_FLOW_INSIGHT",
                        color = colors.primary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                    FeatureHelpIcon(feature = FeatureKey.DASHBOARD_WHALE_INSIGHT, iconSize = 12.dp)
                }
                Text(
                    text = "[FREE]",
                    color = colors.dimText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = signal.emoji, fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = signal.label,
                        color = when (signal) {
                            WhaleSignal.BULLISH_HEAVY, WhaleSignal.BULLISH -> colors.primary
                            WhaleSignal.BEARISH_HEAVY, WhaleSignal.BEARISH -> colors.danger
                            WhaleSignal.NEUTRAL -> colors.amber
                        },
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                    Text(
                        text = signal.description,
                        color = colors.textPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = onLearnMore) {
                    Text(
                        text = "[ How_calculated? ]",
                        color = colors.dimText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                    )
                }
                TextButton(onClick = onUpgrade) {
                    Text(
                        text = "[ Live_feed_→ ]",
                        color = colors.amber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }
}
