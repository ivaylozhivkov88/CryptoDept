package com.cryptodept.ui.screensaver

import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.min

object HeatmapColorMapper {
    /**
     * Maps 24h change percentage to a color intensity.
     * Positive -> Green shades
     * Negative -> Red shades
     */
    fun getColorForChange(change: Double): Color {
        val intensity = (min(abs(change), 10.0) / 10.0).toFloat()

        return if (change >= 0) {
            // Mix Black with Green
            lerpColor(Color.Black, Color(0xFF00FF41), intensity)
        } else {
            // Mix Black with Red
            lerpColor(Color.Black, Color(0xFFFF3B30), intensity)
        }
    }

    private fun lerpColor(
        start: Color,
        end: Color,
        fraction: Float,
    ): Color =
        Color(
            red = start.red + (end.red - start.red) * fraction,
            green = start.green + (end.green - start.green) * fraction,
            blue = start.blue + (end.blue - start.blue) * fraction,
            alpha = 1f,
        )
}
