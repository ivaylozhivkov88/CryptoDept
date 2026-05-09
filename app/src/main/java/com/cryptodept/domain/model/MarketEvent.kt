package com.cryptodept.domain.model

sealed class MarketEvent {
    data class TickerUpdate(
        val symbol: String,
        val price: Double,
        val source: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : MarketEvent()

    data class LargeTrade(
        val symbol: String,
        val price: Double,
        val quantity: Double,
        val amountUsd: Double,
        val side: String, // "BUY" or "SELL"
        val timestamp: Long = System.currentTimeMillis()
    ) : MarketEvent()
}
