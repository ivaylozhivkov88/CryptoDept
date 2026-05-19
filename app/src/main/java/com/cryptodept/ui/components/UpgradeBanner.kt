package com.cryptodept.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
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

/**
 * Banner shown when user lacks access to a feature.
 * Design: non-aggressive, educational, easy upgrade path.
 */
@Composable
fun UpgradeBanner(
    featureName: String,
    description: String,
    requiredTier: String,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTerminalColors.current
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, colors.amber, RectangleShape)
            .background(colors.background.copy(alpha = 0.7f))
            .clickable { onUpgradeClick() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "🔒 $featureName",
            color = colors.amber,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = description,
            color = colors.textPrimary,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Requires $requiredTier access",
            color = colors.dimText,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "[ TAP_TO_UNLOCK ]",
            color = colors.primary,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "From $0.99/day",
            color = colors.dimText,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
        )
    }
}

/**
 * Compact upgrade chip — fits in lists or as inline indicator.
 */
@Composable
fun UpgradeChip(
    label: String = "PRO",
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTerminalColors.current
    
    Surface(
        color = colors.amber.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, colors.amber),
        shape = RectangleShape,
        modifier = modifier
            .clickable { onClick() },
    ) {
        Text(
            text = "🔒 $label",
            color = colors.amber,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
