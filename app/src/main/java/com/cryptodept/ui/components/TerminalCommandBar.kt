package com.cryptodept.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.ui.theme.LocalSoundManager
import com.cryptodept.service.SoundManager
import com.cryptodept.ui.theme.LocalTerminalColors // ЗАМЕНЕНО

@Composable
fun TerminalCommandBar(
    onCommandEntered: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val soundManager = LocalSoundManager.current
    val colors = LocalTerminalColors.current // ВЗЕМАМЕ ТЕКУЩИТЕ ЦВЕТОВЕ
    var command by remember { mutableStateOf("") }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(Color.Black)
            .border(1.dp, colors.dimText.copy(alpha = 0.3f))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = ">",
            color = colors.primary,
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
            modifier = Modifier.padding(end = 8.dp)
        )

        BasicTextField(
            value = command,
            onValueChange = {
                if (it.length > command.length) {
                    soundManager?.playSound(SoundManager.SOUND_CLICK)
                }
                command = it
            },
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp
            ),
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (command.isNotBlank()) {
                        onCommandEntered(command)
                        command = ""
                    }
                }
            ),
            decorationBox = { innerTextField ->
                if (command.isEmpty()) {
                    Text(
                        text = "ENTER COMMAND (HELP, NEWS, CHART BTC)...",
                        color = colors.dimText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                }
                innerTextField()
            }
        )
    }
}