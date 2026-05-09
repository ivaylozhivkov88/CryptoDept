package com.cryptodept.ui.screensaver

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.domain.algo.TreemapItem
import com.cryptodept.domain.algo.TreemapPartition
import com.cryptodept.ui.components.crt.CRTOverlay
import com.cryptodept.ui.theme.LocalTerminalColors
import java.util.*

@Composable
fun HeatmapScreensaverScreen(items: List<TreemapItem>) {
    val colors = LocalTerminalColors.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    val partitioner = remember { TreemapPartition() }
    val rects =
        remember(items, screenWidth, screenHeight) {
            partitioner.squarify(items, screenWidth.value, screenHeight.value)
        }

    val textMeasurer = rememberTextMeasurer()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            rects.forEach { rect ->
                val color = HeatmapColorMapper.getColorForChange(rect.item.change24h)

                // Draw background
                drawRect(
                    color = color,
                    topLeft = Offset(rect.x, rect.y),
                    size = Size(rect.width, rect.height),
                )

                // Draw border
                drawRect(
                    color = Color.Black.copy(alpha = 0.5f),
                    topLeft = Offset(rect.x, rect.y),
                    size = Size(rect.width, rect.height),
                    style = Stroke(width = 1f),
                )

                // Only draw text if the box is large enough
                if (rect.width > 40 && rect.height > 20) {
                    val symbolText = rect.item.symbol
                    val changeText = String.format(Locale.US, "%.1f%%", rect.item.change24h)

                    val textSize = (minOf(rect.width / 4, rect.height / 3, 20f)).sp

                    drawText(
                        textMeasurer = textMeasurer,
                        text = "$symbolText\n$changeText",
                        style =
                            TextStyle(
                                color = Color.White,
                                fontSize = textSize,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            ),
                        topLeft = Offset(rect.x + 4, rect.y + 4),
                        size = Size(rect.width - 8, rect.height - 8),
                    )
                }
            }
        }

        CRTOverlay()
    }
}
