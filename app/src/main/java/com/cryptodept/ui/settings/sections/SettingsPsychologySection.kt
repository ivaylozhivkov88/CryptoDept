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
fun SettingsPsychologySection(
    tiltProtectionEnabled: Boolean,
    onTiltProtectionEnabledChange: (Boolean) -> Unit
) {
    val colors = LocalTerminalColors.current

    Column {
        Text(
            text = ">>> PSYCHOLOGY_&_TILT_PROTECTION",
            color = colors.dimText,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // TILT PROTECTION
        SettingRow("TILT_PROTECTION", "Auto-locks terminal during emotional volatility", tiltProtectionEnabled) {
            onTiltProtectionEnabledChange(it)
        }
    }
}
