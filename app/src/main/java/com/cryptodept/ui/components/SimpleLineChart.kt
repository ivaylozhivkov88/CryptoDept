package com.cryptodept.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.domain.model.OHLCData
import com.cryptodept.ui.theme.JetBrainsMono
import com.cryptodept.ui.theme.LocalTerminalColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SimpleLineChart(
    data: List<OHLCData>,
    modifier: Modifier = Modifier,
    lineColor: Color? = null
) {
    if (data.isEmpty()) return
    val colors = LocalTerminalColors.current
    val resolvedLineColor = lineColor ?: colors.primary

    // Ensure data is sorted by timestamp for a correct timeline
    val sortedData = remember(data) { data.sortedBy { it.timestamp } }
    val textMeasurer = rememberTextMeasurer()
    val prices = remember(sortedData) { sortedData.map { it.close } }
    val maxPrice = remember(prices) { prices.maxOrNull() ?: 1.0 }
    val minPrice = remember(prices) { prices.minOrNull() ?: 0.0 }
    val range = remember(maxPrice, minPrice) { (maxPrice - minPrice).coerceAtLeast(0.0001) }
    
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val labelStyle = TextStyle(
        color = colors.dimText,
        fontSize = 8.sp,
        fontFamily = JetBrainsMono
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(sortedData) {
                detectTapGestures { offset ->
                    val labelPadding = 50.dp.toPx()
                    val chartWidth = size.width - labelPadding
                    if (offset.x <= chartWidth) {
                        val index = (offset.x / chartWidth * (prices.size - 1)).toInt().coerceIn(0, prices.size - 1)
                        selectedIndex = index
                    }
                }
            }
    ) {
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
            drawLine(
                color = colors.grid.copy(alpha = 0.15f),
                start = Offset(0f, y),
                end = Offset(chartWidth, y),
                strokeWidth = 1f
            )
            val priceLabel = maxPrice - (i * (range / gridCount))
            val labelText = if (priceLabel >= 1.0) String.format(Locale.US, "$%.2f", priceLabel) else String.format(Locale.US, "$%.4f", priceLabel)
            drawText(
                textMeasurer = textMeasurer,
                text = labelText,
                style = labelStyle,
                topLeft = Offset(chartWidth + 8f, y - 6.dp.toPx())
            )
        }

        // 2. Draw Price Path
        val path = Path()
        prices.forEachIndexed { index, price ->
            val x = (index.toFloat() / (pointCount - 1)) * chartWidth
            val y = chartHeight - (((price - minPrice) / range).toFloat() * chartHeight)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(path = path, color = resolvedLineColor, style = Stroke(width = 2f))

        // 3. Draw Selected Point / Tooltip
        selectedIndex?.let { index ->
            val price = prices[index]
            val x = (index.toFloat() / (pointCount - 1)) * chartWidth
            val y = chartHeight - (((price - minPrice) / range).toFloat() * chartHeight)
            
            drawCircle(color = colors.amber, radius = 4.dp.toPx(), center = Offset(x, y))
            drawLine(color = colors.amber.copy(alpha = 0.5f), start = Offset(x, 0f), end = Offset(x, chartHeight), strokeWidth = 1f)
            
            val tooltipText = String.format(Locale.US, "$%.2f", price)
            val tooltipLayout = textMeasurer.measure(tooltipText, labelStyle.copy(color = colors.background, fontWeight = FontWeight.Bold))
            
            drawRect(
                color = colors.amber,
                topLeft = Offset(x - tooltipLayout.size.width / 2 - 4f, y - 20.dp.toPx()),
                size = Size(tooltipLayout.size.width.toFloat() + 8f, tooltipLayout.size.height.toFloat() + 4f)
            )
            
            drawText(
                textMeasurer = textMeasurer,
                text = tooltipText,
                style = labelStyle.copy(color = colors.background, fontWeight = FontWeight.Bold),
                topLeft = Offset(x - tooltipLayout.size.width / 2, y - 18.dp.toPx())
            )
        }

        // 4. Draw Time Labels (Bottom Side)
        val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
        val labelCount = 5
        val timeStep = (pointCount - 1) / (labelCount - 1).coerceAtLeast(1)
        
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
    color: Color? = null
) {
    if (prices.isEmpty()) return
    val colors = LocalTerminalColors.current
    val resolvedColor = color ?: colors.primary

    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1000),
        label = "sparkline_anim"
    )

    val maxPrice = remember(prices) { prices.maxOrNull() ?: 1.0 }
    val minPrice = remember(prices) { prices.minOrNull() ?: 0.0 }
    val range = remember(maxPrice, minPrice) { (maxPrice - minPrice).coerceAtLeast(0.0001) }

    Canvas(modifier = modifier.fillMaxSize()) {
        val path = Path()
        val pointCount = prices.size
        
        prices.forEachIndexed { index, price ->
            val x = (index.toFloat() / (pointCount - 1)) * size.width
            val y = size.height - (((price - minPrice) / range).toFloat() * size.height)
            
            if (index == 0) {
                path.moveTo(x, y)
            } else if (index <= (pointCount - 1) * animationProgress) {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = resolvedColor,
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}
