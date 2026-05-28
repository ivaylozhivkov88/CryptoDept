package com.cryptodept.ui.screensaver

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.domain.model.CoinPrice
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.viewmodel.BloombergWallViewModel
import kotlinx.coroutines.delay
import java.util.*

@Composable
fun BloombergWallScreen(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    viewModel: BloombergWallViewModel = hiltViewModel()
) {
    val colors = LocalTerminalColors.current
    val topCoinsRaw by viewModel.topCoins.collectAsState()
    val topCoins = remember(topCoinsRaw) { topCoinsRaw.take(30) } // Limit to top 30 for performance (Q-007)
    val lastUpdate by viewModel.lastUpdate.collectAsState()

    // Map CoinPrice list to the expected priceData format for existing sub-composables
    val priceData = remember(topCoins) {
        topCoins.associate { it.symbol.uppercase() to Pair(it.currentPrice, it.priceChangePercentage24h) }
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
            SystemHealth(lastUpdate)
            Spacer(modifier = Modifier.height(8.dp))

            // ═══════════════════════════════════════
            // 3. LIVE PRICES TAPE — 15 Валути (MATRIX-STYLE FALLING)
            // ═══════════════════════════════════════
            MatrixPricesTicker(priceData, topCoins.map { it.symbol.uppercase() })

            Spacer(modifier = Modifier.height(2.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            // ═══════════════════════════════════════
            // 3. MARKET DOMINANCE — TOP 5
            // ═══════════════════════════════════════
            MarketDominanceSection(topCoins)

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            // ═══════════════════════════════════════
            // 4. PRICE CHART STATUS
            // ═══════════════════════════════════════
            PriceChartStatus(priceData)

            // 5. LIVE DATA INDICATOR
            Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = ">>> REAL-TIME TERMINAL FEEDS ACTIVE <<<",
                    color = colors.primary.copy(alpha = 0.6f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MarketHeadlinesTicker() {
    val colors = LocalTerminalColors.current
    val headlines =
        listOf(
            "► BTC market structure optimization complete",
            "► Institutional capital flow monitoring active",
            "► Multi-agent neural orchestration synced",
            "► Cross-exchange liquidity clusters identified",
            "► Volatility risk engine: NOMINAL",
            "► Terminal status: ELITE_MODE_ACTIVE",
        )

    var offset by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        while (true) {
            delay(30)
            offset += 3f
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
    val avgChange = if (priceData.isNotEmpty()) priceData.values.map { it.second }.average() else 0.0
    val label =
        when {
            avgChange > 2.0 -> "STRONG BULLISH"
            avgChange > 0.5 -> "BULLISH"
            avgChange >= -0.5 -> "NEUTRAL"
            avgChange >= -2.0 -> "BEARISH"
            else -> "STRONG BEARISH"
        }
    val pct = (avgChange / 10.0).toFloat().coerceIn(-1f, 1f)

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(18.dp)
                .background(colors.background)
                .padding(horizontal = 16.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize().background(colors.grid.copy(alpha = 0.12f)))

        val fillColor =
            when {
                avgChange > 0.5 -> colors.primary
                avgChange < -0.5 -> colors.danger
                else -> colors.amber
            }

        val fillWidthPercent = (0.5f + pct / 2f).coerceIn(0f, 1f)

        Box(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fillWidthPercent)
                    .background(fillColor.copy(alpha = 0.9f)),
        )

        Text(
            text = "SENTIMENT: $label (${String.format(Locale.US, "%+.2f", avgChange)}%)",
            color = colors.textPrimary,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
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
            ageSec < 60 -> "SYNCHRONIZED"
            ageSec < 300 -> "LAG_DETECTED"
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
                    "SYNCHRONIZED" -> colors.primary
                    "LAG_DETECTED" -> colors.amber
                    else -> colors.danger
                }
            Text("DATA_STREAM: $status", color = statusColor, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            Text(
                "T: ${String.format(Locale.US, "%tT", Date(lastUpdateMillis))}",
                color = colors.dimText,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
fun MatrixPricesTicker(priceData: Map<String, Pair<Double, Double>>, symbols: List<String>) {
    val colors = LocalTerminalColors.current
    val density = LocalDensity.current

    if (symbols.isEmpty()) return

    val columns = 12
    val rowHeightPx = with(density) { 18.dp.toPx() }

    val displayList =
        remember(priceData, symbols) {
            List(columns) {
                val list = mutableListOf<String>()
                val data =
                    symbols.map { s ->
                        val (p, c) = priceData[s] ?: Pair(0.0, 0.0)
                        val arrow = if (c >= 0) "▲" else "▼"
                        "$s ${String.format(Locale.US, "%.2f", p)} $arrow${String.format(Locale.US, "%.1f", kotlin.math.abs(c))}%"
                    }
                while (list.size < 100) {
                    list.addAll(data.shuffled())
                }
                list
            }
        }

    val offsets =
        remember { mutableStateListOf<Float>().apply { repeat(columns) { add((0..(rowHeightPx.toInt() * 20)).random().toFloat()) } } }
    val speeds = remember { List(columns) { (30..120).random() / 60f } }

    LaunchedEffect(Unit) {
        while (true) {
            delay(16L)
            for (i in 0 until columns) {
                val new = offsets[i] + speeds[i]
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
fun MarketDominanceSection(coins: List<CoinPrice>) {
    val colors = LocalTerminalColors.current
    var dominanceOffset by remember { mutableFloatStateOf(0f) }

    val top5 = remember(coins) {
        val totalCap = coins.sumOf { it.marketCap }
        if (totalCap <= 0) emptyList()
        else coins.take(5).map { it.symbol.uppercase() to ((it.marketCap / totalCap) * 100).toFloat() }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60)
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
            fontSize = 11.sp,
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
                    top5.forEach { (symbol, percentage) ->
                        val barWidth = percentage / 100f

                        Column {
                            Text(
                                text = "$symbol: ${String.format(Locale.US, "%.1f", percentage)}%",
                                color = colors.primary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Box(
                                modifier =
                                    Modifier
                                        .width((barWidth * 160).dp)
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

                Box(
                    modifier =
                        Modifier
                            .width(80.dp)
                            .height(30.dp)
                            .background(Color(0xFF0a0a0a)),
                ) {
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
                    text = String.format(Locale.US, "%+.2f", change) + "%",
                    color = changeColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.align(Alignment.CenterVertically)) {
            Text("FEEDS: ENCRYPTED", color = colors.dimText, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            Text(
                "SYNC: REAL-TIME SECURE",
                color = colors.dimText,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
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
                .background(_colors.grid.copy(alpha = 0.5f)),
    )
}
