package com.cryptodept.ui.settings.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.ui.theme.LocalTerminalColors

@Composable
fun SettingsTradingSection() {
    val colors = LocalTerminalColors.current

    Column {
        Text(
            text = ">>> TRADING_PARAMETERS",
            color = colors.dimText,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Text(
            text = "NO_CONFIGURABLE_TRADING_PARAMS_IN_CURRENT_BUILD",
            color = colors.dimText,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}
