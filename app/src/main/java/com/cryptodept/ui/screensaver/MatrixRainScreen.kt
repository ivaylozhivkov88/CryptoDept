package com.cryptodept.ui.screensaver

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cryptodept.ui.theme.WallStreetGreen
import kotlin.random.Random

@Composable
fun MatrixRainScreen(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    val japaneseChars = "ｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜﾝ".toList()
    val englishChars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ$#@%&*".toList()

    val columnsCount = 20
    val fontSize = 36.sp
    val trailLength = 15

    // Състояние на колоните (дропките)
    val drops = remember {
        mutableStateListOf<Float>().apply {
            repeat(columnsCount) { add(Random.nextFloat() * -100f) }
        }
    }

    // Скорост на падане (намалена за по-естествен вид)
    val speeds = remember {
        List(columnsCount) { Random.nextFloat() * 0.12f + 0.05f }
    }

    val textMeasurer = rememberTextMeasurer()
    var tick by remember { mutableLongStateOf(0L) }

    // Анимационен цикъл
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis {
                tick++
                for (i in 0 until columnsCount) {
                    drops[i] += speeds[i]
                    // Рестартиране на колоната, когато излезе от екрана
                    if (drops[i] > 40f) drops[i] = -20f
                }
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                // Събуждане на приложението при докосване на екрана
                detectTapGestures { onDismiss() }
            }
    ) {
        val columnWidth = size.width / columnsCount
        val fontSizePx = fontSize.toPx()

        for (i in 0 until columnsCount) {
            val x = i * columnWidth
            for (j in 0..trailLength) {
                val charY = (drops[i] - j) * fontSizePx

                if (charY > -fontSizePx && charY < size.height) {
                    // Избор на символ (динамично се променя спрямо "tick")
                    val isJapanese = ((i + j + (tick / 10).toInt()) % 5) != 0
                    val charPool = if (isJapanese) japaneseChars else englishChars
                    val charIndex = (i + j + (tick / 5).toInt()) % charPool.size
                    val char = charPool[charIndex].toString()

                    val alpha = (1f - (j / trailLength.toFloat())).coerceIn(0f, 1f)
                    val color = if (j == 0) Color.White else WallStreetGreen.copy(alpha = alpha)

                    drawText(
                        textMeasurer = textMeasurer,
                        text = char,
                        topLeft = androidx.compose.ui.geometry.Offset(x, charY),
                        style = TextStyle(
                            color = color,
                            fontSize = fontSize,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            shadow = if (j == 0) androidx.compose.ui.graphics.Shadow(
                                color = WallStreetGreen,
                                blurRadius = 12f
                            ) else null
                        )
                    )
                }
            }
        }
    }
}