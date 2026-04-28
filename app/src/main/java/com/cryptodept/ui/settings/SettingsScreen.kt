package com.cryptodept.ui.settings

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontFamily
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
    val phosphorMode by viewModel.phosphorMode.collectAsState()

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

        // AUDIO
        SettingRow("AUDIO_FEEDBACK", "Synthesized sound effects", soundEnabled) {
            viewModel.setSoundsEnabled(it)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // NOTIFICATIONS
        SettingRow("PUSH_ALERTS", "High-confidence signal alerts", notificationsEnabled) {
            viewModel.setNotificationsEnabled(it)
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