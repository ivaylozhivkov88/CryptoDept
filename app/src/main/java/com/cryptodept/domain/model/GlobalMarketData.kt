package com.cryptodept.domain.model

data class GlobalMarketData(
    val activeCoins: Int,
    val totalMarketCap: Double,
    val totalVolume: Double,
    val marketCapChangePercentage24h: Double,
    val btcDominance: Double,
    val ethDominance: Double,
)
