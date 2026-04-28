package com.cryptodept.domain.model

enum class AlertDirection {
    ABOVE, BELOW
}

data class Alert(
    val id: Int = 0,
    val coinId: String,
    val coinSymbol: String,
    val targetPrice: Double,
    val direction: AlertDirection,
    val isActive: Boolean = true,
    val isTriggered: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)