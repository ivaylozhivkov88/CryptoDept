package com.cryptodept.ui.screensaver

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
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

// High-performance data structure
private class FastRainDrop(
    val column: Int,
    var y: Float,
    var speed: Float,
    var length: Int,
    var characters: List<String>,
    var segmentLimit: Float // When to vanish
)

@Composable
fun MatrixRainScreen(
    modifier: Modifier = Modifier,
    btcPrice: String = "FETCHING...",
    allPrices: List<String> = emptyList(),
    riskScore: Int = 50,
    onDismiss: () -> Unit,
) {
    val matrixChars = remember {
        ("\uFF66\uFF67\uFF68\uFF69\uFF6A\uFF6B\uFF6C\uFF6D\uFF6E\uFF6F" +
        "\uFF70\uFF71\uFF72\uFF73\uFF74\uFF75\uFF76\uFF77\uFF78\uFF79" +
        "\uFF7A\uFF7B\uFF7C\uFF7D\uFF7E\uFF7F\uFF80\uFF81\uFF82\uFF83" +
        "\uFF84\uFF85\uFF86\uFF87\uFF88\uFF89\uFF8A\uFF8B\uFF8C\uFF8D" +
        "\uFF8E\uFF8F\uFF90\uFF91\uFF92\uFF93\uFF94\uFF95\uFF96\uFF97" +
        "\uFF98\uFF99\uFF9A\uFF9B\uFF9C\uFF9D" + "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ₿Ξ$").map { it.toString() }
    }

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }
    
    val fontSize = 13.sp
    val fontSizePx = with(density) { fontSize.toPx() }
    val textMeasurer = rememberTextMeasurer()
    
    val columnWidth = fontSizePx * 0.9f
    val columnsCount = (screenWidth / columnWidth).toInt()

    // Initialize drops with segment limits for "appearing/disappearing" effect
    val drops = remember {
        List(columnsCount) { i ->
            FastRainDrop(
                column = i,
                y = Random.nextFloat() * -screenHeight,
                speed = Random.nextFloat() * 5f + 5f,
                length = Random.nextInt(8, 20),
                characters = List(25) { matrixChars.random() },
                segmentLimit = Random.nextFloat() * screenHeight * 0.8f + (screenHeight * 0.2f)
            )
        }
    }

    // Optimized frame cycle with explicit 60fps cap (Q-002)
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while(true) {
            tick++
            delay(16L) // ~60 FPS
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) { detectTapGestures { onDismiss() } }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Force redraw on each tick
            val _trigger = tick
            
            drops.forEach { drop ->
                // Update position
                drop.y += drop.speed
                
                // If drop passes its segment limit OR bottom of screen, reset to a new random start
                if (drop.y > drop.segmentLimit || drop.y > size.height + (drop.length * fontSizePx)) {
                    drop.y = -fontSizePx * drop.length
                    drop.segmentLimit = Random.nextFloat() * size.height * 0.7f + (size.height * 0.3f)
                    drop.speed = Random.nextFloat() * 5f + 5f
                }

                val x = drop.column * columnWidth
                
                // Draw trailing characters
                for (i in 0 until drop.length) {
                    val charY = drop.y - (i * fontSizePx)
                    if (charY < -fontSizePx || charY > size.height) continue

                    val alpha = (1f - (i.toFloat() / drop.length)).coerceIn(0.1f, 1f)
                    val charColor = if (i == 0) Color.White else Color(0xFF00FF41).copy(alpha = alpha)

                    // Draw text with minimal styling overhead
                    drawText(
                        textMeasurer = textMeasurer,
                        text = drop.characters[i % drop.characters.size],
                        topLeft = androidx.compose.ui.geometry.Offset(x, charY),
                        style = TextStyle(
                            color = charColor,
                            fontSize = fontSize,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (i == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                }
                
                // Occasionally swap a character for glimmer effect (staggered)
                if (Random.nextFloat() > 0.98f) {
                    drop.characters = drop.characters.toMutableList().apply {
                        set(Random.nextInt(size), matrixChars.random())
                    }
                }
            }
        }

        // --- OPTIMIZED CLOCK & COIN ROTATION ---
        var currentTime by remember { mutableStateOf(Calendar.getInstance().time) }
        var currentCoinIndex by remember { mutableIntStateOf(0) }
        
        LaunchedEffect(Unit) {
            while(true) {
                currentTime = Calendar.getInstance().time
                delay(1000) // Update clock every second
            }
        }

        LaunchedEffect(allPrices.size) {
            if (allPrices.isNotEmpty()) {
                while(true) {
                    delay(5000) // Rotate every 5 seconds
                    currentCoinIndex = (currentCoinIndex + 1) % allPrices.size
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 64.dp, end = 24.dp)
                .background(Color.Black)
                .border(1.dp, Color(0xFF00FF41).copy(alpha = 0.6f))
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = java.text.SimpleDateFormat("HH:mm:ss", Locale.US).format(currentTime),
                color = Color(0xFF00FF41),
                fontFamily = FontFamily.Monospace,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                style = TextStyle(shadow = Shadow(Color(0xFF00FF41), blurRadius = 8f)),
                softWrap = false
            )
            
            HorizontalDivider(color = Color(0xFF00FF41).copy(alpha = 0.2f), thickness = 0.5.dp)

            val displayCoinInfo = if (allPrices.isNotEmpty()) {
                allPrices[currentCoinIndex % allPrices.size]
            } else {
                "BTC PRICE: $btcPrice"
            }

            Text(
                text = displayCoinInfo,
                color = Color(0xFFFFA500),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "RISK_LEVEL: $riskScore/100",
                color = if (riskScore > 75) Color.Red else Color(0xFF00FF41),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
