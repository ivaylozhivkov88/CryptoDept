package com.cryptodept.ui.dashboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.ui.theme.LocalTerminalColors

@Composable
fun SemiCircleGauge(
    value: Float,
    label: String,
    verdict: String,
    verdictColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalTerminalColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(
            text = label, 
            color = colors.primary.copy(alpha = 0.7f), 
            fontSize = 11.sp, 
            fontFamily = FontFamily.Monospace, 
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.8f),
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 4.dp.toPx()
                val radius = (size.width / 2) - 8.dp.toPx()
                val center = Offset(size.width / 2, size.height - 4.dp.toPx())
                val segments = listOf(colors.danger, colors.danger.copy(alpha = 0.7f), colors.amber, colors.primary.copy(alpha = 0.7f), colors.primary)
                val startBaseAngle = 180f
                val sweepTotal = 180f
                val segmentSweep = sweepTotal / segments.size
                val gap = 4f
                segments.forEachIndexed { i, color ->
                    drawArc(
                        color = color,
                        startAngle = startBaseAngle + (i * segmentSweep) + (gap / 2),
                        sweepAngle = segmentSweep - gap,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                val indicatorAngle = 180f + (value / 100f * 180f)
                val rad = Math.toRadians(indicatorAngle.toDouble())
                val dotX = center.x + radius * Math.cos(rad).toFloat()
                val dotY = center.y + radius * Math.sin(rad).toFloat()
                drawCircle(color = Color.Black, radius = 5.dp.toPx(), center = Offset(dotX, dotY))
                drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(dotX, dotY), style = Stroke(width = 1.2.dp.toPx()))
            }
            Column(
                modifier = Modifier.offset(y = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "${value.toInt()}", color = Color.White, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, fontSize = 20.sp)
                Text(text = verdict, color = verdictColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
