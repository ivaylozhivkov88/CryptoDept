package com.cryptodept.data.remote.model

import androidx.annotation.Keep

@Keep
data class CloudTerminalState(
    var marketData: Map<String, CloudMarketData> = emptyMap(),
    var whaleAlerts: List<CloudWhaleAlert> = emptyList(),
    var macroBriefing: CloudMacroBriefing? = null,
    var agentStatuses: Map<String, String> = emptyMap(),
    var agentReports: Map<String, String> = emptyMap(),
    var sessionBriefs: Map<String, String> = emptyMap(),
    var aiNarrative: String = "",
    var systemStatus: String = "OPERATIONAL",
    var version: String = "1.5.0",
    var lastUpdateTimestamp: Long = 0L
)

@Keep
data class CloudMarketData(
    var id: String = "",
    var symbol: String = "",
    var currentPrice: Double = 0.0,
    var priceChange24h: Double = 0.0,
    var marketCap: Double = 0.0,
    var volume24h: Double = 0.0,
    var source: String = "Cloud",
    var lastUpdated: Long = 0L,
    // Нови полета за технически анализ от облака
    var rsi: Double = 50.0,
    var macdSignal: String = "NEUTRAL",
    var trend: String = "NEUTRAL",
    var riskScore: Int = 50
)

@Keep
data class CloudWhaleAlert(
    var asset: String = "",
    var amountUsd: Double = 0.0,
    var transactionType: String = "", // e.g. "EXCHANGE_INFLOW", "WHALE_MOVE"
    var timestamp: Long = 0L,
    var explorerUrl: String = ""
)

@Keep
data class CloudMacroBriefing(
    var narrative: String = "",
    var fearGreedIndex: Int = 50,
    var globalMarketCapUsd: Double = 0.0,
    var globalMarketCapChange: Double = 0.0,
    var btcDominance: Double = 0.0,
    var altcoinSeasonIndex: Int = 50,
    var ethGasGwei: Int = 0,
    var riskScore: Int = 0,
    var globalLiquidityUsd: Double = 0.0,
    var gasPrediction: String = "NOW", // NEW: M1.2
    var liquidations1h: CloudLiquidationSnapshot = CloudLiquidationSnapshot(),
    var liquidations24h: CloudLiquidationSnapshot = CloudLiquidationSnapshot()
)

@Keep
data class CloudLiquidationSnapshot(
    var totalUsd: Double = 0.0,
    var longsUsd: Double = 0.0,
    var shortsUsd: Double = 0.0
)
