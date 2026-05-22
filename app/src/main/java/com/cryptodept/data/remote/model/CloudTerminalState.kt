package com.cryptodept.data.remote.model

import androidx.annotation.Keep

@Keep
data class CloudTerminalState(
    val marketData: Map<String, CloudMarketData> = emptyMap(),
    val whaleAlerts: List<CloudWhaleAlert> = emptyList(),
    val macroBriefing: CloudMacroBriefing? = null,
    val agentStatuses: Map<String, String> = emptyMap(),
    val agentReports: Map<String, String> = emptyMap(), // NEW: E1.2 - Specialized reports
    val aiNarrative: String = "",
    val lastUpdateTimestamp: Long = 0L
)

@Keep
data class CloudMarketData(
    val id: String = "",
    val symbol: String = "",
    val currentPrice: Double = 0.0,
    val priceChange24h: Double = 0.0,
    val marketCap: Double = 0.0,
    val volume24h: Double = 0.0,
    val source: String = "Cloud",
    // Нови полета за технически анализ от облака
    val rsi: Double = 50.0,
    val macdSignal: String = "NEUTRAL",
    val trend: String = "NEUTRAL",
    val riskScore: Int = 50
)

@Keep
data class CloudWhaleAlert(
    val asset: String = "",
    val amountUsd: Double = 0.0,
    val transactionType: String = "", // e.g. "EXCHANGE_INFLOW", "WHALE_MOVE"
    val timestamp: Long = 0L,
    val explorerUrl: String = ""
)

@Keep
data class CloudMacroBriefing(
    val narrative: String = "",
    val fearGreedIndex: Int = 50,
    val globalMarketCapUsd: Double = 0.0,
    val globalMarketCapChange: Double = 0.0,
    val btcDominance: Double = 0.0,
    val altcoinSeasonIndex: Int = 50,
    val ethGasGwei: Int = 0,
    val riskScore: Int = 0,
    val globalLiquidityUsd: Double = 0.0,
    val gasPrediction: String = "NOW", // NEW: M1.2
    val liquidations1h: CloudLiquidationSnapshot = CloudLiquidationSnapshot(),
    val liquidations24h: CloudLiquidationSnapshot = CloudLiquidationSnapshot()
)

@Keep
data class CloudLiquidationSnapshot(
    val totalUsd: Double = 0.0,
    val longsUsd: Double = 0.0,
    val shortsUsd: Double = 0.0
)
