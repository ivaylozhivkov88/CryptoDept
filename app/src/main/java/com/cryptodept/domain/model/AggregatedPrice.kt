package com.cryptodept.domain.model

data class AggregatedPrice(
    val coinId: String,
    val binancePrice: Double?,
    val krakenPrice: Double?,
    val coinbasePrice: Double?,
    val coincapPrice: Double?,
    val coinpaprikaPrice: Double?,
    val consensusPrice: Double,        // Медиана
    val maxDeviationPercent: Double,   // (max - min) / median * 100
    val isReliable: Boolean,           // true if deviation < 0.5% AND sources >= 3
    val sourcesCount: Int,
    val timestamp: Long = System.currentTimeMillis()
)
