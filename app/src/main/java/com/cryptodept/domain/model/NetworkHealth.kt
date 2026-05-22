package com.cryptodept.domain.model

data class NetworkHealth(
    val btcHashrate: String,
    val btcMempool: String,
    val ethGas: String,
    val fearGreedIndex: Int,
    val fearGreedLabel: String,
    val socialPulse: Int = 50,
    val socialPulseLabel: String = "NEUTRAL",
    val lastUpdated: Long = System.currentTimeMillis()
)
