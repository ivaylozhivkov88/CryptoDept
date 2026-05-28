package com.cryptodept.ui.tutorial

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.cryptodept.ui.dashboard.components.*
import com.cryptodept.domain.model.*
import com.cryptodept.ui.theme.CryptoDeptTheme
import com.cryptodept.domain.tier.AccessTier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Идеални макети за Onboarding-а. Използват се вместо статични PNG файлове.
 */

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun MockStep1_Ticker() {
    val samplePrices = listOf(
        CoinPrice(id = "bitcoin", symbol = "BTC", name = "Bitcoin", currentPrice = 103245.50, priceChange24h = 1200.0, priceChangePercentage24h = 2.34, marketCap = 0.0, totalVolume = 0.0, high24h = 0.0, low24h = 0.0, lastUpdated = 0L, isTracked = true)
    )
    CryptoDeptTheme {
        DashboardTickerSection(
            currentPrices = samplePrices,
            networkHealth = null,
            pricesLastUpdated = System.currentTimeMillis(),
            isCloudLive = true,
            showVerdict = false
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun MockStep2_Gauges() {
    val macro = MacroIntelligence(52.0, 0.0, 20, 2.5e12, 45, 1.2e13, "STABLE", LiquidationSnapshot(0.0, 0.0, 0.0, 0L), LiquidationSnapshot(0.0, 0.0, 0.0, 0L))
    val health = NetworkHealth("80 EH/s", "10 vB", "20 Gwei", 65, "Greed")
    CryptoDeptTheme {
        DashboardMarketOverviewSection(
            currentPrices = emptyList(),
            networkHealth = health,
            macroIntelligence = macro,
            pricesLastUpdated = 0L,
            onCoinClick = {},
            modifier = Modifier
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun MockStep3_Sentinel() {
    CryptoDeptTheme {
        OracleNarrativeStrip(
            narrative = "VERDICT: BULLISH CONFLUENCE DETECTED. BTC DEFENDING 60K SUPPORT.",
            onExpand = {},
            modifier = Modifier
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun MockStep4_Status() {
    CryptoDeptTheme {
        AgentStatusLine(
            statuses = emptyMap(),
            modifier = Modifier
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun MockStep5_Whale() {
    CryptoDeptTheme {
        DashboardWhaleSection(
            signal = WhaleSignal.BULLISH,
            alerts = emptyList(),
            lastUpdatedMs = System.currentTimeMillis(),
            navController = androidx.navigation.compose.rememberNavController(),
            tier = AccessTier.PRO,
            modifier = Modifier
        )
    }
}
