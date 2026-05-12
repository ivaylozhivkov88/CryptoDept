package com.cryptodept.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.domain.model.OHLCData
import com.cryptodept.ui.theme.JetBrainsMono
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SimpleLineChart(
    data: List<OHLCData>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF00FF41)
) {
    if (data.isEmpty()) return

    // Ensure data is sorted by timestamp for a correct timeline
    val sortedData = remember(data) { data.sortedBy { it.timestamp } }
    val textMeasurer = rememberTextMeasurer()
    val prices = sortedData.map { it.close }
    val maxPrice = prices.maxOrNull() ?: 1.0
    val minPrice = prices.minOrNull() ?: 0.0
    val range = (maxPrice - minPrice).coerceAtLeast(0.0001)

    val labelStyle = TextStyle(
        color = Color.Gray,
        fontSize = 8.sp,
        fontFamily = JetBrainsMono
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val labelPadding = 50.dp.toPx() // More space for price labels
        val bottomPadding = 24.dp.toPx() // More space for time labels
        
        val chartWidth = size.width - labelPadding
        val chartHeight = size.height - bottomPadding
        
        val pointCount = prices.size
        if (pointCount < 2) return@Canvas

        // 1. Draw Grid Lines and Price Labels (Right Side)
        val gridCount = 4
        for (i in 0..gridCount) {
            val y = (chartHeight / gridCount) * i
            
            // Draw horizontal grid line
            drawLine(
                color = Color.Gray.copy(alpha = 0.15f),
                start = Offset(0f, y),
                end = Offset(chartWidth, y),
                strokeWidth = 1f
            )
            
            // Draw price label
            val priceLabel = maxPrice - (i * (range / gridCount))
            val labelText = if (priceLabel >= 1.0) {
                String.format(Locale.US, "$%.2f", priceLabel)
            } else {
                String.format(Locale.US, "$%.4f", priceLabel)
            }
            
            drawText(
                textMeasurer = textMeasurer,
                text = labelText,
                style = labelStyle,
                topLeft = Offset(chartWidth + 8f, y - 6.dp.toPx()) // Better alignment
            )
        }

        // 2. Draw Price Path
        val path = Path()
        prices.forEachIndexed { index, price ->
            val x = (index.toFloat() / (pointCount - 1)) * chartWidth
            val y = chartHeight - (((price - minPrice) / range).toFloat() * chartHeight)
            
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2f)
        )

        // 3. Draw Time Labels (Bottom Side)
        val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
        val labelCount = 5
        val timeStep = (pointCount - 1) / (labelCount - 1)
        
        for (i in 0 until labelCount) {
            val index = (i * timeStep).coerceAtMost(pointCount - 1)
            val item = sortedData[index]
            val x = (index.toFloat() / (pointCount - 1)) * chartWidth
            val timeStr = timeFormat.format(Date(item.timestamp))
            
            drawText(
                textMeasurer = textMeasurer,
                text = timeStr,
                style = labelStyle,
                topLeft = Offset(x - 12.dp.toPx(), chartHeight + 6.dp.toPx())
            )
        }
    }
}

@Composable
fun SimpleSparkline(
    prices: List<Double>,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF00FF41)
) {
    if (prices.isEmpty()) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val maxPrice = prices.maxOrNull() ?: 1.0
        val minPrice = prices.minOrNull() ?: 0.0
        val range = (maxPrice - minPrice).coerceAtLeast(0.0001)

        val path = Path()
        val pointCount = prices.size
        
        prices.forEachIndexed { index, price ->
            val x = (index.toFloat() / (pointCount - 1)) * size.width
            val y = size.height - (((price - minPrice) / range).toFloat() * size.height)
            
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}
