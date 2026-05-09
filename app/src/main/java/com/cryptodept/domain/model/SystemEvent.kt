package com.cryptodept.domain.model

import java.util.UUID

data class SystemEvent(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: EventType,
    val message: String,
    val priority: EventPriority = EventType.toPriority(type),
)

enum class EventType {
    MARKET_SIGNAL, // AI Signals (BUY/SELL)
    PRICE_ALERT, // Price thresholds
    TECHNICAL_LEVEL, // Support/Resistance tests
    NETWORK_HEALTH, // Hashrate, Gas, etc
    SYSTEM_STATUS, // Terminal boots, connection updates
    ;

    companion object {
        fun toPriority(type: EventType): EventPriority =
            when (type) {
                MARKET_SIGNAL -> EventPriority.HIGH
                PRICE_ALERT -> EventPriority.CRITICAL
                TECHNICAL_LEVEL -> EventPriority.MEDIUM
                else -> EventPriority.LOW
            }
    }
}

enum class EventPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
}
