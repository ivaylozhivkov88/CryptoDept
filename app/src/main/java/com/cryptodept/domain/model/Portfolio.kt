package com.cryptodept.domain.model

data class PortfolioEntry(
    val id: String,
    val coinId: String,
    val symbol: String,
    val quantity: Double,
    val averageEntryPrice: Double,
    val addedAt: Long
)

data class PortfolioEntryWithCurrentPrice(
    val entry: PortfolioEntry,
    val currentPrice: Double,
    val pnlUsd: Double,
    val pnlPercent: Double,
    val currentValueUsd: Double
)

data class PortfolioSummary(
    val totalValueUsd: Double,
    val totalCostUsd: Double,
    val totalPnlUsd: Double,
    val totalPnlPercent: Double,
    val entries: List<PortfolioEntryWithCurrentPrice>
)
