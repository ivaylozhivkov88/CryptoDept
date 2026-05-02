package com.cryptodept.ui.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val colors = LocalTerminalColors.current
    val soundEnabled by viewModel.soundsEnabled.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val hapticEnabled by viewModel.hapticEnabled.collectAsState()
    val screensaverTimeout by viewModel.screensaverTimeout.collectAsState()
    val phosphorMode by viewModel.phosphorMode.collectAsState()
    
    val billingViewModel: com.cryptodept.viewmodel.BillingViewModel = hiltViewModel()
    val isPro by billingViewModel.billingManager.isPro.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Text(
            ">>> SYSTEM_SETTINGS_V2",
            color = colors.primary,
            fontFamily = FontFamily.Monospace,
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // PHOSPHOR MODE TOGGLE
        SettingRow("PHOSPHOR_TYPE", "Current: $phosphorMode", true) {
            val nextMode = if (phosphorMode == "GREEN") "AMBER" else "GREEN"
            viewModel.setPhosphorMode(nextMode)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // PRO STATUS
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, if (isPro) colors.primary else colors.amber, RectangleShape)
                .background(if (isPro) colors.primary.copy(alpha = 0.05f) else colors.amber.copy(alpha = 0.05f))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isPro) "CRYPTODEPT PRO ACTIVE" else "CRYPTODEPT FREE TIER",
                        color = if (isPro) colors.primary else colors.amber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (isPro) "Unlimited Terminal Access" else "Limited to 3 Tracked Coins",
                        color = colors.dimText,
                        fontSize = 10.sp
                    )
                }
                
                if (!isPro) {
                    Text(
                        text = "[GO PRO]",
                        color = colors.amber,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { /* Trigger Paywall */ }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // AUDIO
        SettingRow("AUDIO_FEEDBACK", "Synthesized sound effects", soundEnabled) {
            viewModel.setSoundsEnabled(it)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // NOTIFICATIONS
        SettingRow("PUSH_ALERTS", "High-confidence signal alerts", notificationsEnabled) {
            viewModel.setNotificationsEnabled(it)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // HAPTIC
        SettingRow("HAPTIC_FEEDBACK", "Tactile response on interaction", hapticEnabled) {
            viewModel.setHapticEnabled(it)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SCREENSAVER TIMEOUT
        val timeoutLabel = if (screensaverTimeout == 0) "OFF" else "${screensaverTimeout}min"
        SettingRow("SCREENSAVER", "Current timeout: $timeoutLabel", true) {
            val timeouts = listOf(0, 2, 5, 10, 30)
            val nextIdx = (timeouts.indexOf(screensaverTimeout) + 1) % timeouts.size
            viewModel.setScreensaverTimeout(timeouts[nextIdx])
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            border = BorderStroke(1.dp, colors.primary),
            shape = RectangleShape,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary)
        ) {
            Text("< RETURN_TO_CORE", fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun SettingRow(label: String, desc: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = LocalTerminalColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.primary.copy(alpha = 0.2f), RectangleShape)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = colors.primary, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
            Text(desc, color = colors.dimText, fontSize = 10.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.primary,
                checkedTrackColor = colors.primary.copy(alpha = 0.3f),
                uncheckedBorderColor = colors.dimText
            )
        )
    }
}