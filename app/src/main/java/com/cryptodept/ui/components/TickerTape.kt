package com.cryptodept.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.domain.model.CoinPrice
import com.cryptodept.ui.theme.LocalTerminalColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

@Composable
fun TickerTape(
    prices: List<CoinPrice>,
    networkHealth: com.cryptodept.domain.model.NetworkHealth?,
    modifier: Modifier = Modifier
) {
    val colors = LocalTerminalColors.current // ВЗЕМАМЕ ТЕКУЩИТЕ ЦВЕТОВЕ

    val tickerItems = remember(prices, networkHealth) {
        val list = mutableListOf<TickerItem>()
        prices.forEach { coin ->
            list.add(TickerItem.Price(coin))
        }
        networkHealth?.let {
            list.add(TickerItem.Stat("FEAR&GREED: ${it.fearGreedIndex} [${it.fearGreedLabel.uppercase()}]"))
            list.add(TickerItem.Stat("MEMPOOL: ${it.btcMempool}"))
            list.add(TickerItem.Stat("GAS: ${it.ethGas}"))
        }
        List(10) { list }.flatten()
    }

    val listState = rememberLazyListState()
    val currentItems by rememberUpdatedState(tickerItems)

    LaunchedEffect(tickerItems.size) {
        if (tickerItems.isEmpty()) return@LaunchedEffect
        listState.scrollToItem(tickerItems.size / 2)

        while (isActive) {
            listState.scrollBy(3f)
            val firstVisible = listState.firstVisibleItemIndex
            val size = currentItems.size
            if (size > 0 && firstVisible >= (size * 0.8f)) {
                listState.scrollToItem(size / 2)
            }
            delay(16)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(Color(0xFF0A0A0A))
            .testTag("TickerTape")
    ) {
        LazyRow(
            state = listState,
            userScrollEnabled = false,
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(tickerItems) { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (item) {
                        is TickerItem.Price -> {
                            val coin = item.coin
                            // Използваме твърдо червено/зелено за тренд, но primary за текста
                            val trendColor = if (coin.priceChangePercentage24h >= 0) Color(0xFF00FF41) else Color(0xFFFF3B30)
                            val symbol = if (coin.priceChangePercentage24h >= 0) "▲" else "▼"

                            Text(
                                text = "${coin.symbol.uppercase()} $${String.format(Locale.US, "%.2f", coin.currentPrice)} ",
                                color = colors.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$symbol${String.format(Locale.US, "%.2f", kotlin.math.abs(coin.priceChangePercentage24h))}%",
                                color = trendColor,
                                fontSize = 11.sp
                            )
                        }
                        is TickerItem.Stat -> {
                            Text(
                                text = item.text,
                                color = colors.primary,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Text(
                        text = "  ░  ",
                        color = colors.grid.copy(alpha = 0.5f), // Използваме цвета на грида за разделител
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.grid.copy(alpha = 0.3f))
                .align(Alignment.BottomCenter)
        )
    }
}

sealed class TickerItem {
    data class Price(val coin: CoinPrice) : TickerItem()
    data class Stat(val text: String) : TickerItem()
}