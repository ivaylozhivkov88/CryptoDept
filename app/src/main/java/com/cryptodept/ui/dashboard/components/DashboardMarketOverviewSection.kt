package com.cryptodept.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.domain.model.CoinPrice
import com.cryptodept.domain.model.NetworkHealth
import com.cryptodept.domain.model.MacroIntelligence
import com.cryptodept.domain.tier.FeatureKey
import com.cryptodept.ui.components.FeatureHelpIcon
import com.cryptodept.ui.components.FreshnessIndicator
import com.cryptodept.ui.theme.LocalTerminalColors
import java.util.Locale

@Composable
fun DashboardMarketOverviewSection(
    currentPrices: List<CoinPrice>,
    networkHealth: NetworkHealth?,
    macroIntelligence: MacroIntelligence?,
    pricesLastUpdated: Long,
    onCoinClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(155.dp, 105.dp)) {
                networkHealth?.let { FearGreedPieChart3D(value = it.fearGreedIndex.toFloat()) }
                FeatureHelpIcon(
                    feature = FeatureKey.DASHBOARD_SENTIMENT_GAUGE,
                    iconSize = 10.dp,
                    modifier = Modifier.align(Alignment.TopEnd).padding(end = 4.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Box(modifier = Modifier.size(155.dp, 105.dp)) {
                macroIntelligence?.let { AltcoinSeasonGauge(value = it.altcoinSeasonIndex.toFloat()) }
                FeatureHelpIcon(
                    feature = FeatureKey.DASHBOARD_ALTCOIN_SEASON,
                    iconSize = 10.dp,
                    modifier = Modifier.align(Alignment.TopEnd).padding(end = 4.dp)
                )
            }
        }
    }
}

@Composable
fun DashboardWatchlistSection(
    currentPrices: List<CoinPrice>,
    onCoinClick: (String) -> Unit
) {
    val colors = LocalTerminalColors.current
    val tracked = currentPrices.filter { it.isTracked }
    
    if (tracked.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ">>> WATCHLIST_OPERATIVES",
                color = colors.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f).height(1.dp).background(colors.grid.copy(alpha = 0.3f)))
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
        ) {
            items(tracked) { coin ->
                MoverChip(coin, onCoinClick)
            }
        }
    }
}

@Composable
fun DashboardVolatilitySection(
    currentPrices: List<CoinPrice>,
    pricesLastUpdated: Long,
    onCoinClick: (String) -> Unit
) {
    val colors = LocalTerminalColors.current
    val movers = currentPrices.sortedByDescending { Math.abs(it.priceChangePercentage24h) }.take(8)
    
    if (movers.isNotEmpty()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VOLATILITY_SCANNER",
                    color = colors.dimText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
                FreshnessIndicator(lastUpdatedMs = pricesLastUpdated, label = "CYCLES")
            }
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(movers) { coin ->
                    MoverChip(coin, onCoinClick)
                }
            }
        }
    }
}

@Composable
private fun MoverChip(coin: CoinPrice, onClick: (String) -> Unit) {
    val colors = LocalTerminalColors.current
    val color = if (coin.priceChangePercentage24h >= 0) colors.primary else colors.error
    Box(
        modifier = Modifier
            .border(1.dp, color.copy(alpha = 0.3f), RectangleShape)
            .background(color.copy(alpha = 0.05f))
            .clickable { onClick(coin.id) }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(coin.symbol.uppercase(), color = colors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(
                "${if (coin.priceChangePercentage24h >= 0) "+" else ""}${String.format(Locale.US, "%.1f", coin.priceChangePercentage24h)}%",
                color = color,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun FearGreedPieChart3D(value: Float, modifier: Modifier = Modifier) {
    val colors = LocalTerminalColors.current
    val verdict = when {
        value <= 25 -> "EXTREME FEAR"
        value <= 46 -> "FEAR"
        value <= 54 -> "NEUTRAL"
        value <= 75 -> "GREED"
        else -> "EXTREME GREED"
    }
    val verdictColor = when {
        value <= 46 -> colors.danger
        value >= 55 -> colors.primary
        else -> colors.amber
    }
    SemiCircleGauge(
        value = value,
        label = "FEAR & GREED INDEX",
        verdict = verdict,
        verdictColor = verdictColor,
        modifier = modifier
    )
}

@Composable
private fun AltcoinSeasonGauge(value: Float, modifier: Modifier = Modifier) {
    val colors = LocalTerminalColors.current
    val verdict = when {
        value <= 25 -> "BTC SEASON"
        value <= 46 -> "BTC BIAS"
        value <= 54 -> "NEUTRAL"
        value <= 75 -> "ALT BIAS"
        else -> "ALT SEASON"
    }
    val verdictColor = when {
        value <= 46 -> colors.amber
        value >= 55 -> colors.primary
        else -> colors.dimText
    }
    SemiCircleGauge(
        value = value,
        label = "ALTCOIN SEASON",
        verdict = verdict,
        verdictColor = verdictColor,
        modifier = modifier
    )
}
