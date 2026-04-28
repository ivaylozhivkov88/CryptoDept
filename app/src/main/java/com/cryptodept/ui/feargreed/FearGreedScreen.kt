package com.cryptodept.ui.feargreed

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.data.api.FearGreedData
import com.cryptodept.ui.components.TerminalLoadingSkeleton
import com.cryptodept.ui.theme.*
import com.cryptodept.viewmodel.FearGreedUiState
import com.cryptodept.viewmodel.FearGreedViewModel
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun FearGreedScreen(
    viewModel: FearGreedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CRTBlack)
            .padding(16.dp)
    ) {
        Text(
            text = ">>> MARKET_SENTIMENT_ANALYSIS",
            color = WallStreetGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp
        )
        HorizontalDivider(color = GridGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

        when (val state = uiState) {
            is FearGreedUiState.Loading -> {
                TerminalLoadingSkeleton(Modifier.fillMaxSize())
            }
            is FearGreedUiState.Success -> {
                FearGreedContent(state.current, state.history)
            }
            is FearGreedUiState.Error -> {
                Text("ERROR: ${state.message}", color = WallStreetRed, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun FearGreedContent(current: FearGreedData, history: List<FearGreedData>) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Gauge
        FearGreedGauge(current.value.toInt())

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "SENTIMENT: ${current.valueClassification.uppercase()}",
            color = getSentimentColor(current.value.toInt()),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = ">>> HISTORICAL_LOG",
            color = TextGray,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.Start)
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(history) { item ->
                HistoryRow(item)
            }
        }
    }
}

@Composable
fun FearGreedGauge(value: Int) {
    Box(
        modifier = Modifier
            .size(240.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 15.dp.toPx()
            val innerRadius = size.minDimension / 2 - strokeWidth
            
            // Background Arc (divided into colors)
            // Extreme Fear (0-25)
            drawArc(
                color = Color(0xFFFF3B30).copy(alpha = 0.3f),
                startAngle = 180f,
                sweepAngle = 45f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            // Fear (25-45)
            drawArc(
                color = Color(0xFFFF9500).copy(alpha = 0.3f),
                startAngle = 225f,
                sweepAngle = 45f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            // Neutral (45-55)
            drawArc(
                color = Color(0xFFFFCC00).copy(alpha = 0.3f),
                startAngle = 270f,
                sweepAngle = 18f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            // Greed (55-75)
            drawArc(
                color = Color(0xFF4CD964).copy(alpha = 0.3f),
                startAngle = 288f,
                sweepAngle = 36f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            // Extreme Greed (75-100)
            drawArc(
                color = Color(0xFF00FF41).copy(alpha = 0.3f),
                startAngle = 324f,
                sweepAngle = 36f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )

            // Current Value Indicator (Needle)
            val angle = 180f + (value.toFloat() / 100f) * 180f
            val rad = Math.toRadians(angle.toDouble())
            val needleLen = innerRadius + strokeWidth
            val endX = center.x + needleLen * cos(rad).toFloat()
            val endY = center.y + needleLen * sin(rad).toFloat()
            
            drawLine(
                color = WallStreetAmber,
                start = center,
                end = Offset(endX, endY),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
            
            drawCircle(color = WallStreetAmber, radius = 6.dp.toPx())
        }
        
        Text(
            text = value.toString(),
            color = WallStreetGreen,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 80.dp)
        )
    }
}

@Composable
fun HistoryRow(item: FearGreedData) {
    val date = java.text.SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(item.timestamp.toLong() * 1000))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, GridGray)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(date, color = TextGray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Text(item.value, color = WallStreetGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        Text(item.valueClassification, color = getSentimentColor(item.value.toInt()), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}

fun getSentimentColor(value: Int): Color {
    return when {
        value < 25 -> WallStreetRed
        value < 45 -> Color(0xFFFF9500)
        value < 55 -> WallStreetAmber
        else -> WallStreetGreen
    }
}
