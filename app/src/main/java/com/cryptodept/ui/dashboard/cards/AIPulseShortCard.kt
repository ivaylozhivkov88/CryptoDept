package com.cryptodept.ui.dashboard.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import com.cryptodept.ui.components.FeatureHelpIcon
import com.cryptodept.domain.tier.FeatureKey
import com.cryptodept.ui.theme.LocalTerminalColors

/**
 * AI Pulse — short version for Free tier.
 * Pro tier sees full streaming narrative.
 */
@Composable
fun AIPulseShortCard(
    summary: String,
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
                        text = ">>> AI_PULSE",
                        color = colors.primary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                    FeatureHelpIcon(feature = FeatureKey.DASHBOARD_AI_PULSE_SHORT, iconSize = 12.dp)
                }
                Text(
                    text = "[FREE]",
                    color = colors.dimText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp,
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = summary.ifBlank { "Calculating market pulse..." },
                color = colors.textPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            TextButton(
                onClick = onUpgrade,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(
                    text = "[ Full_narrative_→ ]",
                    color = colors.amber,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                )
            }
        }
    }
}
