package com.cryptodept.ui.screensaver

// ...existing code... (removed unused animation import)
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
// ...existing code... (removed unused LazyRow imports)
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// ...existing code... (removed unused em import)
import androidx.compose.ui.geometry.Offset
import com.cryptodept.ui.theme.LocalTerminalColors
import kotlinx.coroutines.delay
import java.util.*
// ...existing code... (removed unused sin import)

// Точна енумерация на 15 валути
val TOP_15_CRYPTOS =
    listOf(
        "BTC" to 103450.0,
        "ETH" to 3245.0,
        "BNB" to 645.0,
        "SOL" to 212.0,
        "XRP" to 2.85,
        "DOGE" to 0.42,
        "ADA" to 1.05,
        "MATIC" to 0.95,
        "LINK" to 28.50,
        "DOT" to 8.25,
        "AVAX" to 42.0,
        "TRX" to 0.29,
        "UNI" to 12.35,
        "ATOM" to 10.50,
        "LTC" to 2100.0,
    )

// Top 5 по дом. капитализация (приблизително от пазарни данни)
val TOP_5_DOMINANCE =
    listOf(
        "BTC" to 48.5f,
        "ETH" to 16.2f,
        "BNB" to 4.1f,
        "SOL" to 3.8f,
        "XRP" to 2.9f,
    )

@Composable
fun BloombergWallScreen(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
) {
    val colors = LocalTerminalColors.current

    val priceData = remember { mutableStateMapOf<String, Pair<Double, Double>>() }
    val lastUpdate = remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        // Init prices
        TOP_15_CRYPTOS.forEach { (symbol, basePrice) ->
            priceData[symbol] = Pair(basePrice, (Random().nextDouble() - 0.5) * 3.0)
        }

        // Update prices ogni 180 secondi
        while (true) {
            delay(180000L)
            TOP_15_CRYPTOS.forEach { (symbol, basePrice) ->
                val currentPrice = basePrice * (0.98 + Random().nextDouble() * 0.04)
                val change = (Random().nextDouble() - 0.5) * 5.0
                priceData[symbol] = Pair(currentPrice, change)
            }
            lastUpdate.value = System.currentTimeMillis()
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.background)
                .clickable { onDismiss() },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ═══════════════════════════════════════
            // 1. TOP BANNER — Market Headlines (FAST SCROLL)
            // ═══════════════════════════════════════
            MarketHeadlinesTicker()

            Spacer(modifier = Modifier.height(2.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            // ═══════════════════════════════════════
            // 2. SENTIMENT & SYSTEM HEALTH
            // ═══════════════════════════════════════
            SentimentBar(priceData)
            Spacer(modifier = Modifier.height(6.dp))
            SystemHealth(lastUpdate.value)
            Spacer(modifier = Modifier.height(8.dp))

            // ═══════════════════════════════════════
            // 3. LIVE PRICES TAPE — 15 Валути (MATRIX-STYLE FALLING)
            // ═══════════════════════════════════════
            MatrixPricesTicker(priceData)

            Spacer(modifier = Modifier.height(2.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            // ═══════════════════════════════════════
            // 3. MARKET DOMINANCE — TOP 5
            // ═══════════════════════════════════════
            MarketDominanceSection()

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            // ═══════════════════════════════════════
            // 4. PRICE CHART STATUS
            // ═══════════════════════════════════════
            PriceChartStatus(priceData)
        }
    }
}

@Composable
fun MarketHeadlinesTicker() {
    val colors = LocalTerminalColors.current
    val headlines =
        listOf(
            "► BTC breaks through $103K resistance",
            "► ETH demand surge in DeFi sector",
            "► Fed signals hawkish stance — market reacts",
            "► Crypto volatility spike expected this week",
            "► BlackRock ETF inflows accelerate",
            "► Altseason indicators flash BULLISH",
        )

    var offset by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        while (true) {
            delay(30) // Fast scroll
            offset += 3f // 3px per frame = rapid movement
            if (offset > 2000f) offset = -100f
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(32.dp)
                .background(colors.background)
                .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.offset(x = with(density) { (-offset).toDp() }),
            horizontalArrangement = Arrangement.spacedBy(48.dp),
        ) {
            repeat(3) {
                // Repeat headlines 3x for seamless scroll
                headlines.forEach { headline ->
                    Text(
                        text = headline,
                        color = colors.amber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        overflow = TextOverflow.Clip,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun SentimentBar(priceData: Map<String, Pair<Double, Double>>) {
    val colors = LocalTerminalColors.current
    // compute simple sentiment from average % change of the TOP_15
    val avgChange = if (priceData.isNotEmpty()) priceData.values.map { it.second }.average() else 0.0
    val label =
        when {
            avgChange > 1.0 -> "STRONG BULLISH"
            avgChange > 0.2 -> "BULLISH"
            avgChange >= -0.2 -> "NEUTRAL"
            avgChange >= -1.0 -> "BEARISH"
            else -> "STRONG BEARISH"
        }
    val pct = (avgChange / 5.0).toFloat().coerceIn(-1f, 1f) // normalize for bar

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(18.dp)
                .background(colors.background)
                .padding(4.dp),
    ) {
        // Background neutral bar
        Box(modifier = Modifier.fillMaxSize().background(colors.grid.copy(alpha = 0.12f)))

        // Center marker and dynamic amber/green fill
        val fillColor =
            when {
                avgChange > 0.2 -> colors.primary
                avgChange < -0.2 -> colors.danger
                else -> colors.amber
            }

        val fillWidth = ((0.5f + pct / 2f) * 100f).coerceIn(0f, 100f)

        Box(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .width((fillWidth).dp)
                    .background(fillColor.copy(alpha = 0.9f)),
        )

        Text(
            text = "SENTIMENT: $label  (avg ${String.format(Locale.US, "%.2f", avgChange)}%)",
            color = colors.textPrimary,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp),
        )
    }
}

@Composable
fun SystemHealth(lastUpdateMillis: Long) {
    val colors = LocalTerminalColors.current
    val ageSec = ((System.currentTimeMillis() - lastUpdateMillis) / 1000).toInt()
    val status =
        when {
            ageSec < 200 -> "OK"
            ageSec < 600 -> "STALE"
            else -> "OFFLINE"
        }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            val statusColor =
                when (status) {
                    "OK" -> colors.primary
                    "STALE" -> colors.amber
                    else -> colors.danger
                }
            Text("SYSTEM FEEDS: $status", color = statusColor, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            Text(
                "Last update: ${String.format(Locale.US, "%tT", Date(lastUpdateMillis))}",
                color = colors.dimText,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
fun MatrixPricesTicker(priceData: Map<String, Pair<Double, Double>>) {
    val colors = LocalTerminalColors.current
    val density = LocalDensity.current

    // Matrix parameters
    val columns = 12
    val rowHeightDp = 18.dp
    val rowHeightPx = with(density) { rowHeightDp.toPx() }

    // Build strings for display from priceData (static until next update)
    val symbols = TOP_15_CRYPTOS.map { it.first }
    val displayList =
        remember(priceData) {
            List(columns) {
                // each column gets a shuffled repeating sequence of price strings
                val list = mutableListOf<String>()
                val data =
                    symbols.map { s ->
                        val (p, c) = priceData[s] ?: Pair(0.0, 0.0)
                        val arrow = if (c >= 0) "▲" else "▼"
                        "$s ${String.format(Locale.US, "%.2f", p)} $arrow${String.format(Locale.US, "%.1f", kotlin.math.abs(c))}%"
                    }
                // repeat to fill
                while (list.size < 100) {
                    list.addAll(data.shuffled())
                }
                list
            }
        }

    // state: vertical offset for each column and speed
    val offsets =
        remember { mutableStateListOf<Float>().apply { repeat(columns) { add((0..(rowHeightPx.toInt() * 20)).random().toFloat()) } } }
    val speeds = remember { List(columns) { (30..120).random() / 60f } }

    LaunchedEffect(Unit) {
        while (true) {
            val frameMs = 16L
            delay(frameMs)
            for (i in 0 until columns) {
                val new = offsets[i] + speeds[i]
                // loop when too large
                offsets[i] = if (new > rowHeightPx * 100) 0f else new
            }
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(colors.background)
                .padding(8.dp),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            for (col in 0 until columns) {
                val list = displayList[col]
                Box(modifier = Modifier.weight(1f)) {
                    val offsetPx = offsets[col]
                    val offsetDp = with(density) { (-offsetPx).toDp() }
                    // draw stack of texts vertically, each shifted by offsetDp
                    for (i in 0 until 40) {
                        val text = list[i % list.size]
                        val yDp = with(density) { (i * rowHeightPx).toDp() }
                        val isHead = (i == 0)
                        Text(
                            text = text,
                            color = if (isHead) colors.primary else colors.primary.copy(alpha = 0.45f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.offset(y = offsetDp + yDp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MarketDominanceSection() {
    val colors = LocalTerminalColors.current
    var dominanceOffset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60) // Slowest scroll - 1px per frame
            dominanceOffset += 1f
            if (dominanceOffset > 2500f) dominanceOffset = -200f
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
    ) {
        Text(
            text = "MARKET CAP DOMINANCE — TOP 5",
            color = colors.amber,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp),
        )

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(Color(0xFF1a1a1a))
                    .padding(horizontal = 4.dp),
        ) {
            Row(
                modifier = Modifier.offset(x = with(LocalDensity.current) { (-dominanceOffset).toDp() }),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(2) {
                    TOP_5_DOMINANCE.forEach { (symbol, percentage) ->
                        val barWidth = percentage / 50f // visual bar width

                        Column {
                            Text(
                                text = "$symbol: ${String.format(Locale.US, "%.1f", percentage)}%",
                                color = colors.primary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Box(
                                modifier =
                                    Modifier
                                        .width((barWidth * 80).dp)
                                        .height(4.dp)
                                        .background(colors.primary.copy(alpha = 0.6f)),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PriceChartStatus(priceData: Map<String, Pair<Double, Double>>) {
    val colors = LocalTerminalColors.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        listOf("BTC", "ETH", "SOL").forEach { symbol ->
            val (_, change) = priceData[symbol] ?: Pair(0.0, 0.0)
            val changeColor = if (change >= 0) colors.primary else colors.danger

            Column {
                Text(
                    text = symbol,
                    color = colors.amber,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )

                // Mini sparkline placeholder
                Box(
                    modifier =
                        Modifier
                            .width(80.dp)
                            .height(30.dp)
                            .background(Color(0xFF0a0a0a)),
                ) {
                    // Simple placeholder visualization (single center line)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawLine(
                            color = if (change >= 0) colors.primary else colors.danger,
                            start = Offset(0f, size.height / 2f),
                            end = Offset(size.width, size.height / 2f),
                            strokeWidth = 2f,
                        )
                    }
                }

                Text(
                    text = String.format(Locale.US, "%.1f", change) + "%",
                    color = changeColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        // Legend explaining colors
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.align(Alignment.CenterVertically)) {
            Text("Legend:", color = colors.dimText, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
            Text(
                "Green = positive % change today; Red = negative % change",
                color = colors.dimText,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
            )
        }
    }
}

@Composable
fun Divider() {
    val _colors = LocalTerminalColors.current
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(_colors.grid),
    )
}
