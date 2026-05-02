package com.cryptodept.ui.screensaver

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.ui.theme.LocalTerminalColors
import kotlinx.coroutines.delay
import java.util.*
import kotlin.random.Random

data class RainColumn(
    var currentY: Float,
    val speed: Float,
    val symbols: List<String>,
    var brightIndex: Int
)

@Composable
fun MatrixRainScreen(
    modifier: Modifier = Modifier,
    btcPrice: String = "$103,450 ▲2.3%", // Defaults for mockup
    riskScore: Int = 45,
    onDismiss: () -> Unit
) {
    val colors = LocalTerminalColors.current
    val cryptoSymbols = listOf("BTC", "ETH", "SOL", "ADA", "XRP", "DOGE", "BNB", "AVAX", "DOT", "LINK", "$", "7", "0")
    
    val textMeasurer = rememberTextMeasurer()
    val columnsCount = 25
    val fontSize = 20.sp
    
    val rainColumns = remember {
        mutableStateListOf<RainColumn>().apply {
            repeat(columnsCount) {
                add(RainColumn(
                    currentY = Random.nextFloat() * -100f,
                    speed = Random.nextFloat() * 0.5f + 0.2f,
                    symbols = List(50) { if (Random.nextFloat() > 0.05f) cryptoSymbols.random() else "$${Random.nextInt(10, 99)}K" },
                    brightIndex = Random.nextInt(0, 50)
                ))
            }
        }
    }

    var tick by remember { mutableLongStateOf(0L) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(50) // 20fps
            tick++
            rainColumns.forEach { col ->
                col.currentY += col.speed
                if (col.currentY > 60f) {
                    col.currentY = -20f
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) { detectTapGestures { onDismiss() } }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val columnWidth = size.width / columnsCount
            val fontSizePx = fontSize.toPx()

            rainColumns.forEachIndexed { i, col ->
                val x = i * columnWidth
                col.symbols.forEachIndexed { j, symbol ->
                    val y = (col.currentY + j) * fontSizePx
                    
                    if (y > -fontSizePx && y < size.height) {
                        // Color logic: White head -> Terminal Green -> Dark Green
                        val distance = Math.abs(col.currentY + j - col.currentY)
                        val color = when {
                            j == 0 -> Color.White
                            distance < 10 -> Color(0xFF00FF41)
                            else -> Color(0xFF001A00)
                        }
                        
                        val alpha = (1f - (distance / 40f)).coerceIn(0f, 1f)

                        drawText(
                            textMeasurer = textMeasurer,
                            text = symbol,
                            topLeft = androidx.compose.ui.geometry.Offset(x, y),
                            style = TextStyle(
                                color = color.copy(alpha = alpha),
                                fontSize = fontSize,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }

        // Bottom Corner Overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            val currentTime = remember { mutableStateOf(Calendar.getInstance().time) }
            LaunchedEffect(tick) {
                if (tick % 20 == 0L) currentTime.value = Calendar.getInstance().time
            }
            
            Text(
                text = String.format(Locale.US, "%tT", currentTime.value),
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 42.sp,
                fontWeight = FontWeight.Light
            )
            Text(
                text = "BTC: $btcPrice",
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp
            )
            Text(
                text = "RISK_LEVEL: $riskScore",
                color = if (riskScore > 70) colors.danger else colors.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
