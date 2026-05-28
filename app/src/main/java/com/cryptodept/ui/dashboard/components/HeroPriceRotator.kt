package com.cryptodept.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cryptodept.domain.model.CoinPrice
import com.cryptodept.ui.effects.shimmerEffect
import com.cryptodept.ui.components.terminalTextStyle
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.util.toCurrency
import kotlinx.coroutines.delay
import androidx.compose.ui.tooling.preview.Preview
import com.cryptodept.ui.theme.CryptoDeptTheme
import java.util.Locale

@Composable
fun HeroPriceRotator(
    prices: List<CoinPrice>,
    onCoinChanged: (CoinPrice) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTerminalColors.current
    val favorites = remember(prices) { 
        val tracked = prices.filter { it.isTracked }
        if (tracked.isNotEmpty()) tracked else prices.take(5).ifEmpty { emptyList() }
    }
    
    if (favorites.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
            Text("SYNCHRONIZING_MARKET_NODE...", color = colors.dimText, fontFamily = FontFamily.Monospace)
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { favorites.size })
    var isPinned by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage, favorites) {
        if (favorites.isNotEmpty() && pagerState.currentPage < favorites.size) {
            onCoinChanged(favorites[pagerState.currentPage])
        }
    }

    // Rotation Loop (Auto-swipe)
    if (!isPinned && favorites.size > 1) {
        LaunchedEffect(favorites.size) {
            while (true) {
                delay(10_000) // Reduced from 30s to 10s for better visibility
                if (!pagerState.isScrollInProgress) {
                    val nextStep = (pagerState.currentPage + 1) % favorites.size
                    pagerState.animateScrollToPage(nextStep)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val currentCoin = favorites[page]
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isPinned = !isPinned }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = "https://assets.coingecko.com/coins/images/${currentCoin.id}/small/${currentCoin.id}.png",
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .background(colors.grid.copy(alpha = 0.1f), CircleShape)
                                .shimmerEffect()
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentCoin.symbol.uppercase(),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                if (currentCoin.isTracked) {
                                    Text(
                                        text = " ★",
                                        color = colors.amber,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            Text(
                                text = currentCoin.name.uppercase(),
                                color = colors.dimText,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    
                    if (isPinned) {
                        Text(
                            text = "[ PINNED ]",
                            color = colors.primary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentCoin.currentPrice.toCurrency(),
                        modifier = Modifier.weight(1f),
                        style = terminalTextStyle(
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            glow = true
                        )
                    )
                    
                    val changeColor = if (currentCoin.priceChangePercentage24h >= 0) colors.primary else colors.danger
                    val arrow = if (currentCoin.priceChangePercentage24h >= 0) "▲" else "▼"
                    
                    Text(
                        text = "$arrow ${String.format(Locale.US, "%.2f", currentCoin.priceChangePercentage24h)}%",
                        color = changeColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        if (favorites.size > 1) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                favorites.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .size(width = 12.dp, height = 2.dp)
                            .background(if (index == pagerState.currentPage) colors.primary else colors.grid.copy(alpha = 0.3f))
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun HeroPriceRotatorPreview() {
    val samplePrices = listOf(
        CoinPrice(
            id = "bitcoin",
            symbol = "BTC",
            name = "Bitcoin",
            currentPrice = 64231.50,
            priceChange24h = 1200.0,
            priceChangePercentage24h = 1.95,
            marketCap = 1200000000000.0,
            totalVolume = 35000000000.0,
            high24h = 65000.0,
            low24h = 63000.0,
            lastUpdated = System.currentTimeMillis(),
            isTracked = true
        ),
        CoinPrice(
            id = "ethereum",
            symbol = "ETH",
            name = "Ethereum",
            currentPrice = 3450.25,
            priceChange24h = -50.0,
            priceChangePercentage24h = -1.45,
            marketCap = 400000000000.0,
            totalVolume = 15000000000.0,
            high24h = 3550.0,
            low24h = 3400.0,
            lastUpdated = System.currentTimeMillis(),
            isTracked = true
        )
    )
    CryptoDeptTheme {
        HeroPriceRotator(
            prices = samplePrices,
            onCoinChanged = {}
        )
    }
}
