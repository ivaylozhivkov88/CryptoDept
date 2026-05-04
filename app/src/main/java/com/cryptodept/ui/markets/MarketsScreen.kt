package com.cryptodept.ui.markets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.cryptodept.domain.model.CoinPrice
import com.cryptodept.ui.components.TerminalErrorOverlay
import com.cryptodept.ui.components.TerminalLoadingSkeleton
import com.cryptodept.ui.navigation.Screen
import com.cryptodept.ui.theme.LocalTerminalColors // ЗАМЕНЕНО
import com.cryptodept.viewmodel.MarketsViewModel
import com.cryptodept.viewmodel.MarketsUiState
import java.util.Locale

@Composable
fun MarketsScreen(
    navController: NavHostController,
    viewModel: MarketsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sentimentMap by viewModel.sentimentMap.collectAsState()
    val colors = LocalTerminalColors.current // ВЗЕМАМЕ ТЕКУЩИТЕ ЦВЕТОВЕ

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background) // Използваме цвета от темата
            .padding(8.dp)
    ) {
        Text(
            text = "--- GLOBAL_MARKET_TERMINAL_v2.0 ---",
            color = colors.primary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        when (val state = uiState) {
            is MarketsUiState.Loading -> {
                LazyColumn {
                    items(10) {
                        TerminalLoadingSkeleton(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
            is MarketsUiState.Success -> {
                MarketsList(state.coins, sentimentMap) { coinId ->
                    navController.navigate(Screen.CoinDetail.createRoute(coinId))
                }
            }
            is MarketsUiState.Error -> {
                TerminalErrorOverlay(message = state.message, onRetry = { viewModel.loadMarkets() })
            }
        }
    }
}

@Composable
fun MarketsList(coins: List<CoinPrice>, sentimentMap: Map<String, com.cryptodept.domain.usecase.SentimentVerdict>, onCoinClick: (String) -> Unit) {
    val colors = LocalTerminalColors.current
    LazyColumn {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(colors.surface),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(" ASSET", modifier = Modifier.weight(1f), color = colors.dimText, fontSize = 10.sp)
                Text("PRICE ", modifier = Modifier.weight(1f), color = colors.dimText, fontSize = 10.sp)
                Text("24H_CHG ", modifier = Modifier.weight(0.8f), color = colors.dimText, fontSize = 10.sp)
                Text("SENT ", modifier = Modifier.weight(0.5f), color = colors.dimText, fontSize = 10.sp)
            }
        }
        items(coins) { coin ->
            MarketRow(coin, onClick = onCoinClick, sentiment = sentimentMap[coin.id])
        }
    }
}

@Composable
fun MarketRow(coin: CoinPrice, onClick: (String) -> Unit, sentiment: com.cryptodept.domain.usecase.SentimentVerdict? = null) {
    val colors = LocalTerminalColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(1.dp, colors.primary.copy(alpha = 0.2f))
            .clickable { onClick(coin.id) }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(coin.symbol.uppercase(), color = colors.primary, fontWeight = FontWeight.Bold)
            Text(coin.name.uppercase(), color = colors.dimText, fontSize = 10.sp)
        }

        Text(
            text = "$${String.format(Locale.US, "%.2f", coin.currentPrice)}",
            modifier = Modifier.weight(1f),
            color = colors.primary
        )

        val trendColor = if (coin.priceChangePercentage24h >= 0) colors.primary else colors.error
        Text(
            text = "${if (coin.priceChangePercentage24h >= 0) "+" else ""}${String.format(Locale.US, "%.2f", coin.priceChangePercentage24h)}%",
            modifier = Modifier.weight(0.8f),
            color = trendColor
        )

        if (sentiment != null) {
            val sentimentColor = when (sentiment) {
                com.cryptodept.domain.usecase.SentimentVerdict.STRONGLY_BULLISH,
                com.cryptodept.domain.usecase.SentimentVerdict.BULLISH -> colors.primary
                com.cryptodept.domain.usecase.SentimentVerdict.STRONGLY_BEARISH,
                com.cryptodept.domain.usecase.SentimentVerdict.BEARISH -> colors.error
                else -> colors.amber
            }
            Text(
                text = sentiment.name.take(4),
                modifier = Modifier.weight(0.5f),
                color = sentimentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            Spacer(modifier = Modifier.weight(0.5f))
        }
    }
}