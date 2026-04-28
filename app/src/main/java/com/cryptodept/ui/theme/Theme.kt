package com.cryptodept.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Дефинираме структурата веднъж и завинаги
data class TerminalColorSet(
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val error: Color,
    val dimText: Color,
    val grid: Color
)

// Единствени дефиниции на сетовете
val GreenColorSet = TerminalColorSet(
    primary = Color(0xFF00FF41),
    secondary = Color(0xFF003B00),
    background = Color(0xFF000000),
    surface = Color(0xFF050505),
    error = Color(0xFFFF3B30),
    dimText = Color(0xFF008F11),
    grid = Color(0xFF003B00)
)

val AmberColorSet = TerminalColorSet(
    primary = Color(0xFFFFB000),
    secondary = Color(0xFF332200),
    background = Color(0xFF000000),
    surface = Color(0xFF050505),
    error = Color(0xFFFF3B30),
    dimText = Color(0xFF996600),
    grid = Color(0xFF332200)
)

val WhiteColorSet = TerminalColorSet(
    primary = Color(0xFFCCCCCC),
    secondary = Color(0xFF333333),
    background = Color(0xFF000000),
    surface = Color(0xFF050505),
    error = Color(0xFFFF3B30),
    dimText = Color(0xFF666666),
    grid = Color(0xFF222222)
)

// CompositionLocal - използваме изрично името на класа TerminalColorSet
val LocalTerminalColors = staticCompositionLocalOf<TerminalColorSet> { GreenColorSet }

enum class PhosphorMode {
    GREEN, AMBER, CRT
}

@Composable
fun CryptoDeptTheme(
    mode: PhosphorMode = PhosphorMode.GREEN,
    content: @Composable () -> Unit
) {
    val terminalColors = when (mode) {
        PhosphorMode.AMBER -> AmberColorSet
        PhosphorMode.CRT -> WhiteColorSet
        else -> GreenColorSet
    }

    val colorScheme = darkColorScheme(
        primary = terminalColors.primary,
        secondary = terminalColors.secondary,
        background = terminalColors.background,
        surface = terminalColors.surface,
        error = terminalColors.error
    )

    CompositionLocalProvider(
        LocalTerminalColors provides terminalColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}