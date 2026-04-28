package com.cryptodept.domain.model

data class CoinPrice(
    val id: String,
    val symbol: String,
    val name: String,
    val currentPrice: Double,
    val priceChange24h: Double,
    val priceChangePercentage24h: Double,
    val marketCap: Double,
    val totalVolume: Double,
    val high24h: Double,
    val low24h: Double,
    val lastUpdated: Long,
    val sparkline: List<Double> = emptyList(),
    // V2 Aggregation Data
    val sourcesCount: Int = 1,
    val maxDeviation: Double = 0.0
)
