package com.cryptodept.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.ui.theme.WallStreetAmber
import com.cryptodept.ui.theme.WallStreetGreen
import com.cryptodept.ui.theme.WallStreetRed
import com.cryptodept.viewmodel.GlobalMarketViewModel
import java.util.Locale

@Composable
fun GlobalMarketBar(
    viewModel: GlobalMarketViewModel = hiltViewModel()
) {
    val data by viewModel.marketData.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(Color.Black)
            .padding(horizontal = 8.dp)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
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
            Text(
                "CONNECTING TO GLOBAL MARKET FEED...",
                color = WallStreetGreen,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun MarketItem(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "$label: ", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        Text(text = value, color = WallStreetAmber, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MarketChange(change: Double) {
    val color = if (change >= 0) WallStreetGreen else WallStreetRed
    val sign = if (change >= 0) "+" else ""
    Text(
        text = "($sign${String.format(Locale.US, "%.1f", change)}%)",
        color = color,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun Separator() {
    Text(text = " | ", color = Color.DarkGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
}
