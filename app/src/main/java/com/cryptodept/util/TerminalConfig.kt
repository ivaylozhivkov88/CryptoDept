package com.cryptodept.util

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object TerminalConfig {
    object UI {
        val CORNER_RADIUS = 0.dp // CRT Style - square
        val BORDER_WIDTH = 1.dp
        val DEFAULT_PADDING = 16.dp
        val SMALL_PADDING = 8.dp
        val TINY_PADDING = 4.dp
        val SCANLINE_HEIGHT = 2.dp
        
        val FONT_SIZE_TINY = 9.sp
        val FONT_SIZE_MICRO = 10.sp
        val FONT_SIZE_SMALL = 11.sp
        val FONT_SIZE_NORMAL = 13.sp
        val FONT_SIZE_MEDIUM = 14.sp
        val FONT_SIZE_LARGE = 16.sp
        val FONT_SIZE_HEADER = 18.sp
        val FONT_SIZE_GIANT = 48.sp

        val SPACER_LARGE = 16.dp
        val SPACER_MEDIUM = 8.dp
        val SPACER_SMALL = 4.dp

        val ICON_SIZE_SMALL = 16.dp
        val ICON_SIZE_NORMAL = 24.dp
    }

    object Strings {
        const val BACK_TO_TOOLS = "< BACK_TO_TOOLS"
        const val REFRESH = "[REFRESH]"
        const val USE_CURRENT = "[USE CURRENT]"
        const val ERROR_GENERIC = "ERROR: SYSTEM_FAILURE"
        const val LOADING = "INITIALIZING..."
    }

    object Animation {
        const val GLITCH_DURATION_MS = 200
        const val TYPEWRITER_SPEED_MS = 30L
        const val CROSSFADE_DURATION_MS = 300
        const val TICKER_SPEED = 3f
    }

    object Interaction {
        const val TOUCH_TARGET_SIZE = 48
        const val IDLE_TIMEOUT_MINUTES = 2
    }
}
