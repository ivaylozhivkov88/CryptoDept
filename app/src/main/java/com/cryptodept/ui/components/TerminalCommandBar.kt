package com.cryptodept.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.ui.theme.LocalTerminalAudioManager
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.util.*
import com.cryptodept.viewmodel.TerminalCommandViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val LocalHapticManager = staticCompositionLocalOf<HapticManager?> { null }
val LocalAnalyticsManager = staticCompositionLocalOf<AnalyticsManager?> { null }

@Composable
fun TerminalCommandBar(
    onCommandEntered: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TerminalCommandViewModel = hiltViewModel(),
) {
    val soundManager = LocalTerminalAudioManager.current
    val colors = LocalTerminalColors.current
    val hapticManager = LocalHapticManager.current
    val analytics = LocalAnalyticsManager.current
    val scope = rememberCoroutineScope()

    var showTooltip by remember { mutableStateOf(false) }
    var command by remember { mutableStateOf("") }
    val commandHistory = remember { mutableStateListOf<String>() }
    var historyIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(Unit) {
        delay(3000)
        showTooltip = true
    }

    val allCommands = remember {
        listOf("HELP", "TUTORIAL", "FOCUSMODE", "CHART", "ANALYSIS", "ALERTS", "NEWS", "SETTINGS", "RISK", "BRIEF", "JOURNAL", "TOOLS", "PREDICT", "PORTFOLIO", "COACH", "SIZER", "PLANNER", "ENTRY", "MTF", "PSYCH", "DERIVS", "BACK", "CLEAR", "VERSION")
    }

    val suggestions = remember(command) {
        if (command.isBlank()) emptyList()
        else {
            val upperCmd = command.uppercase()
            allCommands.filter { (it.startsWith(upperCmd) || it.contains(upperCmd)) && it != upperCmd }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black)
    ) {
        if (showTooltip && command.isEmpty()) {
            Text(
                text = "💡 TIP: TYPE 'HELP' FOR ALL COMMANDS",
                color = colors.amber,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clickable { showTooltip = false },
            )
        }

        if (suggestions.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(suggestions) { suggestion ->
                    SuggestionChip(suggestion) {
                        command = suggestion
                        soundManager?.playSound(TerminalAudioManager.SOUND_CLICK)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(colors.background)
                .border(1.dp, colors.primary.copy(alpha = 0.5f), RectangleShape)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = ">",
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(8.dp))

            BasicTextField(
                value = command,
                onValueChange = {
                    if (it.length > command.length) soundManager?.playSound(TerminalAudioManager.SOUND_CLICK)
                    command = it
                    historyIndex = -1
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("TerminalInput"),
                textStyle = TextStyle(
                    color = colors.primary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp,
                ),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (command.isNotBlank()) {
                            val cmdToExecute = command.trim()
                            if (commandHistory.firstOrNull() != cmdToExecute) {
                                commandHistory.add(0, cmdToExecute)
                                if (commandHistory.size > 10) commandHistory.removeAt(10)
                            }
                            scope.launch {
                                hapticManager?.tick()
                                analytics?.logCommandUsed(cmdToExecute)
                                onCommandEntered(cmdToExecute)
                                command = ""
                                historyIndex = -1
                            }
                        }
                    }
                ),
                decorationBox = { innerTextField ->
                    if (command.isEmpty()) {
                        Text(
                            text = "ENTER_COMMAND",
                            color = colors.dimText.copy(alpha = 0.3f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                        )
                    }
                    innerTextField()
                },
            )

            IconButton(
                onClick = {
                    if (commandHistory.isNotEmpty()) {
                        historyIndex = (historyIndex + 1) % commandHistory.size
                        command = commandHistory[historyIndex]
                        soundManager?.playSound(TerminalAudioManager.SOUND_CLICK)
                    }
                },
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "History",
                    tint = colors.primary.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
fun SuggestionChip(
    text: String,
    onClick: () -> Unit,
) {
    val colors = LocalTerminalColors.current
    Box(
        modifier = Modifier
            .background(colors.surface)
            .border(0.5.dp, colors.grid, RectangleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            color = colors.primary,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
        )
    }
}
