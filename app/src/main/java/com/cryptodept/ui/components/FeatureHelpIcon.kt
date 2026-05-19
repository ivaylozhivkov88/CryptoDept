package com.cryptodept.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.domain.tier.FeatureKey
import com.cryptodept.ui.theme.LocalTerminalColors

/**
 * Compact "?" icon shown next to a feature.
 * 
 * Tap → opens dialog with full description from FeatureKey.description.
 */
@Composable
fun FeatureHelpIcon(
    feature: FeatureKey,
    iconSize: Dp = 18.dp, // INCREASED DEFAULT (Task 2.16)
    modifier: Modifier = Modifier,
) {
    val colors = LocalTerminalColors.current
    var showDialog by remember { mutableStateOf(false) }
    
    IconButton(
        onClick = { showDialog = true },
        modifier = modifier.size(iconSize + 8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.HelpOutline,
            contentDescription = "What is ${feature.displayName}?",
            tint = colors.dimText,
            modifier = Modifier.size(iconSize),
        )
    }
    
    if (showDialog) {
        FeatureHelpDialog(
            feature = feature,
            onDismiss = { showDialog = false },
        )
    }
}

@Composable
private fun FeatureHelpDialog(
    feature: FeatureKey,
    onDismiss: () -> Unit,
) {
    val colors = LocalTerminalColors.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = feature.requiredTier.emoji, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = feature.displayName,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )
            }
        },
        text = {
            Column {
                Text(
                    text = feature.description,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = colors.textPrimary
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Tier indicator
                Surface(
                    color = when (feature.requiredTier.name) {
                        "FREE" -> colors.primary.copy(alpha = 0.2f)
                        "PRO" -> colors.amber.copy(alpha = 0.2f)
                        "ADMIN" -> colors.danger.copy(alpha = 0.2f)
                        else -> colors.dimText.copy(alpha = 0.1f)
                    },
                ) {
                    Text(
                        text = "Available in: ${feature.requiredTier.displayName} tier",
                        color = when (feature.requiredTier.name) {
                            "FREE" -> colors.primary
                            "PRO" -> colors.amber
                            "ADMIN" -> colors.danger
                            else -> colors.dimText
                        },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("[CLOSE]", fontFamily = FontFamily.Monospace, color = colors.primary)
            }
        },
        containerColor = colors.background,
    )
}
