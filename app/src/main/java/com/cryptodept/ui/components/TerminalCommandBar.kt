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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.service.SoundManager
import com.cryptodept.ui.theme.LocalSoundManager
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.util.HapticManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.remember
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalHapticManager = staticCompositionLocalOf<HapticManager?> { null }
val LocalAnalyticsManager = staticCompositionLocalOf<com.cryptodept.util.AnalyticsManager?> { null }

@Composable
fun TerminalCommandBar(
    onCommandEntered: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val soundManager = LocalSoundManager.current
    val colors = LocalTerminalColors.current
    val hapticManager = LocalHapticManager.current
    val analytics = LocalAnalyticsManager.current
    val scope = rememberCoroutineScope()
    
    var command by remember { mutableStateOf("") }
    val commandHistory = remember { mutableStateListOf<String>() }
    var historyIndex by remember { mutableIntStateOf(-1) }
    
    val allCommands = remember {
        listOf(
            "HELP", "CHART", "ANALYSIS", "ALERTS", "NEWS", "SETTINGS", "RISK", "BRIEF", "JOURNAL",
            "TOOLS", "PREDICT", "PORTFOLIO", "COACH", "SIZER", "PLANNER", "ENTRY", "MTF", "PSYCH",
            "DERIVS", "BACK", "CLEAR", "VERSION"
        )
    }
    
    val suggestions = remember(command) {
        if (command.isBlank()) emptyList()
        else allCommands.filter { it.startsWith(command.uppercase()) && it != command.uppercase() }
    }

    var isBlinking by remember { mutableStateOf(false) }
    val blinkAlpha by animateFloatAsState(
        targetValue = if (isBlinking) 0f else 1f,
        animationSpec = tween(durationMillis = 100, easing = LinearEasing),
        finishedListener = { if (it == 0f) isBlinking = false },
        label = "blink"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Autocomplete suggestions row
        if (suggestions.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(suggestions) { suggestion ->
                    SuggestionChip(suggestion) {
                        command = suggestion
                        soundManager?.playSound(SoundManager.SOUND_CLICK)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .graphicsLayer(alpha = if (isBlinking) blinkAlpha else 1f)
                .background(colors.background)
                .border(1.dp, colors.grid.copy(alpha = 0.5f), RectangleShape)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "root@cryptodept:~$ ",
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )

            BasicTextField(
                value = command,
                onValueChange = {
                    if (it.length > command.length) {
                        soundManager?.playSound(SoundManager.SOUND_CLICK)
                    }
                    command = it
                    historyIndex = -1
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("TerminalInput"),
                textStyle = TextStyle(
                    color = colors.primary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                ),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (command.isNotBlank()) {
                            val cmdToExecute = command.trim()
                            
                            // Add to history (max 10)
                            if (commandHistory.firstOrNull() != cmdToExecute) {
                                commandHistory.add(0, cmdToExecute)
                                if (commandHistory.size > 10) commandHistory.removeAt(10)
                            }
                            
                            scope.launch {
                                hapticManager?.tick()
                                analytics?.logCommandUsed(cmdToExecute)
                                isBlinking = true
                                delay(200)
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
                            text = "ENTER COMMAND...",
                            color = colors.dimText.copy(alpha = 0.5f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                    innerTextField()
                }
            )

            // History Button
            IconButton(
                onClick = {
                    if (commandHistory.isNotEmpty()) {
                        historyIndex = (historyIndex + 1) % commandHistory.size
                        command = commandHistory[historyIndex]
                        soundManager?.playSound(SoundManager.SOUND_CLICK)
                    }
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "History",
                    tint = colors.primary.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun SuggestionChip(text: String, onClick: () -> Unit) {
    val colors = LocalTerminalColors.current
    Box(
        modifier = Modifier
            .background(colors.surface)
            .border(1.dp, colors.grid, RectangleShape)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = colors.primary,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
    }
}
