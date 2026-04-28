package com.cryptodept.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cryptodept.domain.model.Alert
import com.cryptodept.domain.model.AlertDirection

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val coinId: String,
    val coinSymbol: String,
    val targetPrice: Double,
    val direction: AlertDirection,
    val isActive: Boolean,
    val isTriggered: Boolean,
    val createdAt: Long
) {
    fun toDomain() = Alert(
        id = id,
        coinId = coinId,
        coinSymbol = coinSymbol,
        targetPrice = targetPrice,
        direction = direction,
        isActive = isActive,
        isTriggered = isTriggered,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(alert: Alert) = AlertEntity(
            id = alert.id,
            coinId = alert.coinId,
            coinSymbol = alert.coinSymbol,
            targetPrice = alert.targetPrice,
            direction = alert.direction,
            isActive = alert.isActive,
            isTriggered = alert.isTriggered,
            createdAt = alert.createdAt
        )
    }
}