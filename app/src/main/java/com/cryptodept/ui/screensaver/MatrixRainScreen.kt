package com.cryptodept.ui.screensaver

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.ui.theme.LocalTerminalColors
import kotlinx.coroutines.delay
import java.util.*
import kotlin.random.Random

class RainDrop(
    val column: Int,
    var y: Float,
    var speed: Float,
    var length: Int,
    var chars: MutableList<String>
)

@Composable
fun MatrixRainScreen(
    modifier: Modifier = Modifier,
    btcPrice: String = "FETCHING...",
    allPrices: List<String> = emptyList(),
    riskScore: Int = 50,
    onDismiss: () -> Unit,
) {
    val colors = LocalTerminalColors.current
    // Add Katakana characters for more authentic Matrix look using Unicode escapes
    val matrixChars = ("01ABCDEFGHIJKLMNOPQRSTUVWXYZ₿Ξ$" + 
        "\uFF66\uFF67\uFF68\uFF69\uFF6A\uFF6B\uFF6C\uFF6D\uFF6E\uFF6F" + 
        "\uFF70\uFF71\uFF72\uFF73\uFF74\uFF75\uFF76\uFF77\uFF78\uFF79" + 
        "\uFF7A\uFF7B\uFF7C\uFF7D\uFF7E\uFF7F\uFF80\uFF81\uFF82\uFF83" + 
        "\uFF84\uFF85\uFF86\uFF87\uFF88\uFF89\uFF8A\uFF8B\uFF8C\uFF8D" + 
        "\uFF8E\uFF8F\uFF90\uFF91\uFF92\uFF93\uFF94\uFF95\uFF96\uFF97" + 
        "\uFF98\uFF99\uFF9A\uFF9B\uFF9C\uFF9D").map { it.toString() }
    
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    val fontSize = 14.sp
    val fontSizePx = with(density) { fontSize.toPx() }
    
    val textMeasurer = rememberTextMeasurer()
    
    val columnWidth = fontSizePx * 0.8f // Narrower columns for more density
    val columnsCount = (screenWidth / columnWidth).toInt()

    // State for rain drops
    val drops = remember { 
        mutableStateListOf<RainDrop>().apply {
            repeat(columnsCount) { i ->
                add(
                    RainDrop(
                        column = i,
                        y = Random.nextFloat() * -screenHeight * 2f, // Spread out vertically
                        speed = Random.nextFloat() * 6f + 3f, // Vary speeds
                        length = Random.nextInt(10, 35), // Longer trails
                        chars = MutableList(35) { matrixChars.random() }
                    )
                )
            }
        }
    }
    
    var tick by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(40)
            tick++
            drops.forEach { drop ->
                drop.y += drop.speed
                if (drop.y > screenHeight + (drop.length * fontSizePx)) {
                    drop.y = -fontSizePx * drop.length
                    drop.speed = Random.nextFloat() * 4f + 4f
                }
                // Randomly change some characters in the trail for "glimmer"
                if (Random.nextFloat() > 0.8f) {
                    val idx = Random.nextInt(drop.chars.size)
                    drop.chars[idx] = matrixChars.random()
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
        // Use tick here to force recomposition
        val currentTick = tick
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dummy = currentTick // Force read
            drops.forEach { drop ->
                val x = drop.column * columnWidth
                for (i in 0 until drop.length) {
                    val charY = drop.y - (i * fontSizePx)
                    if (charY < -fontSizePx || charY > size.height) continue

                    val alpha = (1f - (i.toFloat() / drop.length)).coerceIn(0f, 1f)
                    val charColor = if (i == 0) {
                        Color.White // Glowing head
                    } else {
                        Color(0xFF00FF41).copy(alpha = alpha) // Classic Matrix Green fading
                    }

                    drawText(
                        textMeasurer = textMeasurer,
                        text = drop.chars[i % drop.chars.size],
                        topLeft = androidx.compose.ui.geometry.Offset(x, charY),
                        style = TextStyle(
                            color = charColor,
                            fontSize = fontSize,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (i == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                }
            }
        }

        // --- ELITE CLOCK OVERLAY ---
        val currentTime = remember { mutableStateOf(Calendar.getInstance().time) }
        LaunchedEffect(tick) {
            if (tick % 25 == 0L) currentTime.value = Calendar.getInstance().time
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .background(Color.Black.copy(alpha = 0.8f))
                .border(1.dp, Color(0xFF00FF41).copy(alpha = 0.5f))
                .padding(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = java.text.SimpleDateFormat("HH:mm:ss", Locale.US).format(currentTime.value),
                color = Color(0xFF00FF41),
                fontFamily = FontFamily.Monospace,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color(0xFF00FF41),
                        blurRadius = 10f
                    )
                ),
                softWrap = false
            )
            
            Text(
                text = "BTC PRICE: $btcPrice",
                color = Color(0xFFFFA500),
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "RISK_LEVEL: $riskScore/100",
                color = if (riskScore > 75) Color.Red else Color(0xFF00FF41),
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
        }
    }
}
