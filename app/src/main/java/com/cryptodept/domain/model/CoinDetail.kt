package com.cryptodept.domain.model

data class CoinDetail(
    val id: String,
    val symbol: String,
    val name: String,
    val description: String,
    val homepage: String,
    val currentPrice: Double,
    val marketCap: Double,
    val totalVolume: Double,
    val high24h: Double,
    val low24h: Double,
    val priceChangePercentage24h: Double,
    val isTracked: Boolean = false,
    val sparkline: List<Double>,
    val markets: List<MarketTicker>
)

data class MarketTicker(
    val exchange: String,
    val pair: String,
    val price: Double,
    val volume: Double,
    val tradeUrl: String?
)
