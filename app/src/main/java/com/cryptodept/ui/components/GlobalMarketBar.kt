package com.cryptodept.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.ui.theme.*
import com.cryptodept.viewmodel.GlobalMarketViewModel
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun GlobalMarketBar(viewModel: GlobalMarketViewModel = hiltViewModel()) {
    val data by viewModel.marketData.collectAsState()
    val colors = LocalTerminalColors.current
    val scrollState = rememberScrollState()

    // Auto-scroll logic
    LaunchedEffect(data) {
        if (data != null) {
            while (true) {
                val maxScroll = scrollState.maxValue
                if (maxScroll > 0) {
                    // Slower scroll than main ticker: ~40ms per pixel
                    scrollState.animateScrollTo(
                        value = maxScroll,
                        animationSpec = tween(
                            durationMillis = maxScroll * 40,
                            easing = LinearEasing
                        )
                    )
                    delay(1000)
                    scrollState.scrollTo(0)
                }
                delay(100)
            }
        }
    }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(24.dp)
                .background(colors.background)
                .padding(horizontal = 8.dp)
                .horizontalScroll(scrollState, enabled = true)
                .semantics { contentDescription = "Global Market Summary" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        data?.let { market ->
            MarketItem("CAP", "$${String.format(Locale.US, "%.2fT", market.totalMarketCap / 1e12)}")
            MarketChange(market.marketCapChangePercentage24h)
            Separator()
            MarketItem("VOL", "$${String.format(Locale.US, "%.2fB", market.totalVolume / 1e9)}")
            Separator()
            MarketItem("BTC_DOM", "${String.format(Locale.US, "%.1f", market.btcDominance)}%")
            Separator()
            MarketItem("ETH_DOM", "${String.format(Locale.US, "%.1f", market.ethDominance)}%")
            Separator()
            MarketItem("COINS", market.activeCoins.toString())
        } ?: run {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "CONNECTING TO GLOBAL MARKET FEED...",
                    color = colors.primary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "[RETRY]",
                    color = colors.amber,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.clickable { viewModel.refreshData() }
                )
            }
        }
    }
}

@Composable
private fun MarketItem(
    label: String,
    value: String,
) {
    val colors = LocalTerminalColors.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(text = "$label: ", color = colors.dimText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        Text(text = value, color = colors.amber, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MarketChange(change: Double) {
    val colors = LocalTerminalColors.current
    val color = if (change >= 0) colors.primary else colors.danger
    val sign = if (change >= 0) "+" else ""
    Text(
        text = "($sign${String.format(Locale.US, "%.1f", change)}%)",
        color = color,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun Separator() {
    Text(text = " | ", color = LocalTerminalColors.current.grid, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
}
