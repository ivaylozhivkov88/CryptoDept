package com.cryptodept.ui.settings.sections

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.domain.tutorial.TutorialTargetId
import com.cryptodept.ui.settings.SettingRow
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.ui.tutorial.tutorialTarget

@Composable
fun SettingsTerminalSection(
    phosphorMode: String,
    screensaverTimeout: Int,
    powerUserMode: Boolean,
    forceShowAllFeatures: Boolean,
    onPhosphorModeChange: (String) -> Unit,
    onScreensaverTimeoutChange: (Int) -> Unit,
    onPowerUserModeChange: (Boolean) -> Unit,
    onForceShowAllFeaturesChange: (Boolean) -> Unit,
    onNavigateToGlossary: () -> Unit,
    onRestartTutorial: () -> Unit
) {
    val colors = LocalTerminalColors.current

    Column {
        Text(
            ">>> TERMINAL_CONFIGURATION",
            color = colors.dimText,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // PHOSPHOR MODE TOGGLE
        SettingRow(
            label = "PHOSPHOR_TYPE",
            desc = "Current: $phosphorMode",
            checked = true,
            modifier = Modifier.tutorialTarget(TutorialTargetId.SETTINGS_THEME)
        ) {
            val nextMode = if (phosphorMode == "GREEN") "AMBER" else "GREEN"
            onPhosphorModeChange(nextMode)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SCREENSAVER TIMEOUT
        val timeoutLabel = if (screensaverTimeout == 0) "OFF" else "${screensaverTimeout}min"
        SettingRow("SCREENSAVER", "Current timeout: $timeoutLabel", true) {
            val timeouts = listOf(0, 2, 5, 10, 30)
            val nextIdx = (timeouts.indexOf(screensaverTimeout) + 1) % timeouts.size
            onScreensaverTimeoutChange(timeouts[nextIdx])
        }

        Spacer(modifier = Modifier.height(16.dp))

        // POWER USER
        SettingRow("POWER_USER_MODE", "Enable advanced tools & FFT scans", powerUserMode) {
            onPowerUserModeChange(it)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // PROGRESSIVE DISCLOSURE OVERRIDE
        SettingRow("SHOW_ALL_FEATURES", "Override progressive disclosure", forceShowAllFeatures) {
            onForceShowAllFeaturesChange(it)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // GLOSSARY
        Box(
            modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, colors.primary.copy(alpha = 0.2f), RectangleShape)
                .tutorialTarget(TutorialTargetId.SETTINGS_GLOSSARY)
                .clickable { onNavigateToGlossary() }
                .padding(12.dp),
        ) {
            Column {
                Text("CRYPTO_GLOSSARY", color = colors.primary, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                Text("Learn key crypto and trading terms", color = colors.dimText, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // RESTART TUTORIAL
        Box(
            modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, colors.primary.copy(alpha = 0.2f), RectangleShape)
                .tutorialTarget(TutorialTargetId.SETTINGS_REPLAY_TUTORIAL)
                .clickable { onRestartTutorial() }
                .padding(12.dp),
        ) {
            Column {
                Text("RESTART_TUTORIAL", color = colors.primary, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                Text("Reset first-run onboarding sequence", color = colors.dimText, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }
    }
}
