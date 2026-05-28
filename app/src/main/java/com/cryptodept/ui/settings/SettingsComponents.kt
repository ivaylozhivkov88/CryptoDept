package com.cryptodept.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.ui.theme.LocalTerminalColors

@Composable
fun SettingRow(
    label: String,
    desc: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = LocalTerminalColors.current
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .border(1.dp, colors.primary.copy(alpha = 0.2f), RectangleShape)
                .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = colors.primary, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
            Text(desc, color = colors.dimText, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = colors.primary,
                    checkedTrackColor = colors.primary.copy(alpha = 0.3f),
                    uncheckedBorderColor = colors.dimText,
                ),
        )
    }
}
