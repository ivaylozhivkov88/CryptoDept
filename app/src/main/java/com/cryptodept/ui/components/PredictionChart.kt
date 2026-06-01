package com.cryptodept.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.domain.model.OHLCData
import com.cryptodept.domain.model.PricePrediction
import com.cryptodept.ui.theme.JetBrainsMono
import com.cryptodept.ui.theme.LocalTerminalColors
import java.util.Locale

@Composable
fun PredictionChart(
    historicalData: List<OHLCData>,
    prediction: PricePrediction,
    modifier: Modifier = Modifier
) {
    val colors = LocalTerminalColors.current
    if (historicalData.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        color = colors.dimText,
        fontSize = 8.sp,
        fontFamily = JetBrainsMono
    )

    val targets = listOf(
        prediction.prediction1h,
        prediction.prediction4h,
        prediction.prediction24h,
        prediction.prediction7d
    )

    val allPrices = historicalData.map { it.close } + targets.map { it.mid }
    val maxPrice = allPrices.maxOrNull() ?: 1.0
    val minPrice = allPrices.minOrNull() ?: 0.0
    val range = (maxPrice - minPrice).coerceAtLeast(0.0001)

    Canvas(modifier = modifier.fillMaxSize()) {
        val labelPadding = 60.dp.toPx() // Increased for price labels
        val bottomPadding = 30.dp.toPx()
        val chartWidth = size.width - labelPadding
        val chartHeight = size.height - bottomPadding

        val histSize = historicalData.size
        val predSize = targets.size
        val totalSize = histSize + predSize

        // 1. Draw Grid and Price Labels (RIGHT)
        val gridCount = 4
        for (i in 0..gridCount) {
            val y = (chartHeight / gridCount) * i
            val priceLabel = maxPrice - (i * (range / gridCount))
            
            drawLine(
                color = colors.grid.copy(alpha = 0.15f),
                start = Offset(0f, y),
                end = Offset(chartWidth, y),
                strokeWidth = 1f
            )

            drawText(
                textMeasurer = textMeasurer,
                text = String.format(Locale.US, "$%.2f", priceLabel),
                style = labelStyle,
                topLeft = Offset(chartWidth + 4.dp.toPx(), y - 6.dp.toPx())
            )
        }

        // 2. Draw Historical Line (Solid Green)
        val histPath = Path()
        historicalData.forEachIndexed { index, ohlc ->
            val x = (index.toFloat() / (totalSize - 1)) * chartWidth
            val y = chartHeight - (((ohlc.close - minPrice) / range).toFloat() * chartHeight)
            if (index == 0) histPath.moveTo(x, y) else histPath.lineTo(x, y)
        }
        drawPath(path = histPath, color = colors.primary, style = Stroke(width = 2.dp.toPx()))

        // 3. Draw Prediction Line (Dashed Amber)
        val predPath = Path()
        val lastHistX = ((histSize - 1).toFloat() / (totalSize - 1)) * chartWidth
        val lastHistY = chartHeight - (((historicalData.last().close - minPrice) / range).toFloat() * chartHeight)
        predPath.moveTo(lastHistX, lastHistY)

        targets.forEachIndexed { index, target ->
            val x = ((histSize + index).toFloat() / (totalSize - 1)) * chartWidth
            val y = chartHeight - (((target.mid - minPrice) / range).toFloat() * chartHeight)
            predPath.lineTo(x, y)
            
            drawCircle(
                color = colors.amber,
                radius = 3.dp.toPx(),
                center = Offset(x, y)
            )
        }

        drawPath(
            path = predPath,
            color = colors.amber,
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        )

        // 4. Vertical separator and labels (BOTTOM)
        drawLine(
            color = colors.amber.copy(alpha = 0.5f),
            start = Offset(lastHistX, 0f),
            end = Offset(lastHistX, chartHeight),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
        )

        drawText(
            textMeasurer = textMeasurer,
            text = "PAST",
            style = labelStyle,
            topLeft = Offset(lastHistX / 2 - 10.dp.toPx(), chartHeight + 4.dp.toPx())
        )

        drawText(
            textMeasurer = textMeasurer,
            text = "FORECAST",
            style = labelStyle.copy(color = colors.amber),
            topLeft = Offset(lastHistX + (chartWidth - lastHistX) / 2 - 20.dp.toPx(), chartHeight + 4.dp.toPx())
        )
        
        // Final Target Price Label
        val finalTarget = targets.last()
        val finalY = chartHeight - (((finalTarget.mid - minPrice) / range).toFloat() * chartHeight)
        drawText(
            textMeasurer = textMeasurer,
            text = ">>> TARGET",
            style = labelStyle.copy(color = colors.amber, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            topLeft = Offset(chartWidth - 60.dp.toPx(), finalY - 14.dp.toPx())
        )
    }
}
