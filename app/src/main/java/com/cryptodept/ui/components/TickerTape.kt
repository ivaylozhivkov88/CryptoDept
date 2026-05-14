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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.domain.model.CoinPrice
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.util.TerminalConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

@Composable
fun TickerTape(
    prices: List<CoinPrice>,
    networkHealth: com.cryptodept.domain.model.NetworkHealth?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTerminalColors.current // OBTAIN CURRENT THEME COLORS

    val tickerItems =
        remember(prices, networkHealth) {
            val list = mutableListOf<TickerItem>()
            prices.forEach { coin ->
                list.add(TickerItem.Price(coin))
            }
            networkHealth?.let {
                list.add(TickerItem.Stat("FEAR&GREED: ${it.fearGreedIndex} [${it.fearGreedLabel.uppercase()}]"))
                list.add(TickerItem.Stat("MEMPOOL: ${it.btcMempool}"))
                list.add(TickerItem.Stat("GAS: ${it.ethGas}"))
            }
            if (list.isEmpty()) emptyList() else List(50) { list }.flatten() // LARGE ENOUGH FOR SEAMLESS LOOP
        }

    val listState = rememberLazyListState()
    val currentItems by rememberUpdatedState(tickerItems)

    LaunchedEffect(tickerItems.size) {
        if (tickerItems.isEmpty()) return@LaunchedEffect
        
        // Start from middle to allow scrolling both ways
        listState.scrollToItem(tickerItems.size / 2)

        while (isActive) {
            listState.scrollBy(TerminalConfig.Animation.TICKER_SPEED)
            
            // Loop logic: if we reach 90% of the list, jump back to middle
            val firstVisible = listState.firstVisibleItemIndex
            val size = currentItems.size
            if (size > 0 && firstVisible >= (size * 0.9f)) {
                listState.scrollToItem(size / 2)
            }
            delay(16)
        }
    }

    val accessibilityDescription = remember(prices) {
        val topPrices = prices.take(5).joinToString { "${it.symbol} at ${String.format(Locale.US, "$%.2f", it.currentPrice)}" }
        "Market Ticker showing: $topPrices"
    }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(colors.background)
                .semantics { contentDescription = accessibilityDescription }
                .testTag("TickerTape"),
    ) {
        LazyRow(
            state = listState,
            userScrollEnabled = true,
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(tickerItems) { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (item) {
                        is TickerItem.Price -> {
                            val coin = item.coin
                            // Use theme-aware colors for trend
                            val trendColor = if (coin.priceChangePercentage24h >= 0) colors.primary else colors.error
                            val symbol = if (coin.priceChangePercentage24h >= 0) "▲" else "▼"

                            Text(
                                text = "${coin.symbol.uppercase()} $${String.format(Locale.US, "%.2f", coin.currentPrice)} ",
                                color = colors.primary,
                                fontSize = TerminalConfig.UI.FONT_SIZE_SMALL,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                            )
                            Text(
                                text = "$symbol${String.format(Locale.US, "%.2f", kotlin.math.abs(coin.priceChangePercentage24h))}%",
                                color = trendColor,
                                fontSize = TerminalConfig.UI.FONT_SIZE_SMALL,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                        is TickerItem.Stat -> {
                            Text(
                                text = item.text,
                                color = colors.primary,
                                fontSize = TerminalConfig.UI.FONT_SIZE_SMALL,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                    Text(
                        text = "  ░  ",
                        color = colors.grid.copy(alpha = 0.5f), // Use grid color for dividerелител
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.grid.copy(alpha = 0.3f))
                    .align(Alignment.BottomCenter),
        )
    }
}

sealed class TickerItem {
    data class Price(
        val coin: CoinPrice,
    ) : TickerItem()

    data class Stat(
        val text: String,
    ) : TickerItem()
}
