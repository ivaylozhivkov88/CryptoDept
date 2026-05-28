package com.cryptodept.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.navigation.NavController
import com.cryptodept.domain.model.WhaleSignal
import com.cryptodept.domain.tier.AccessTier
import com.cryptodept.ui.components.FreshnessIndicator
import com.cryptodept.ui.theme.LocalTerminalColors

@Composable
fun DashboardWhaleSection(
    signal: WhaleSignal,
    alerts: List<com.cryptodept.data.remote.model.CloudWhaleAlert>,
    lastUpdatedMs: Long,
    navController: NavController,
    tier: AccessTier
) {
    val colors = LocalTerminalColors.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(0.5.dp, colors.grid.copy(alpha = 0.2f), RectangleShape)
            .background(Color.Black.copy(alpha = 0.3f))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "WHALE_TRACKER_DATA",
                color = colors.dimText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            FreshnessIndicator(lastUpdatedMs = lastUpdatedMs, label = "FLOW")
        }

        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        when (signal) {
                            WhaleSignal.BULLISH -> colors.primary
                            WhaleSignal.BEARISH -> colors.danger
                            else -> colors.amber
                        },
                        RectangleShape
                    )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "SIGNAL: ${signal.name}",
                color = Color.White,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            
            Spacer(Modifier.weight(1f))
            
            Text(
                text = "[+]",
                color = colors.primary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.clickable { navController.navigate(com.cryptodept.ui.navigation.Screen.WhaleTracker.route) }
            )
        }
    }
}
