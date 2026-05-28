package com.cryptodept.ui.settings.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.ui.settings.SettingRow
import com.cryptodept.ui.theme.LocalTerminalColors

@Composable
fun SettingsNotificationsSection(
    soundEnabled: Boolean,
    notificationsEnabled: Boolean,
    hapticEnabled: Boolean,
    onSoundEnabledChange: (Boolean) -> Unit,
    onNotificationsEnabledChange: (Boolean) -> Unit,
    onHapticEnabledChange: (Boolean) -> Unit
) {
    val colors = LocalTerminalColors.current

    Column {
        Text(
            text = ">>> NOTIFICATIONS_&_SENSORY",
            color = colors.dimText,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // AUDIO
        SettingRow("AUDIO_FEEDBACK", "Synthesized sound effects", soundEnabled) {
            onSoundEnabledChange(it)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // NOTIFICATIONS
        SettingRow("PUSH_ALERTS", "High-confidence signal alerts", notificationsEnabled) {
            onNotificationsEnabledChange(it)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // HAPTIC
        SettingRow("HAPTIC_FEEDBACK", "Tactile response on interaction", hapticEnabled) {
            onHapticEnabledChange(it)
        }
    }
}
