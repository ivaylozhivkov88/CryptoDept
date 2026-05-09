package com.cryptodept.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cryptodept.domain.model.Alert
import com.cryptodept.domain.model.AlertCondition
import com.cryptodept.domain.model.AlertDirection
import com.cryptodept.domain.model.AlertLogicOperator
import com.cryptodept.domain.model.CompositeAlert
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.collections.immutable.toImmutableList

@Entity(
    tableName = "alerts",
    indices = [Index(value = ["coinId"])],
)
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val coinId: String,
    val coinSymbol: String,
    val targetPrice: Double,
    val direction: AlertDirection,
    val isActive: Boolean,
    val isTriggered: Boolean,
    val createdAt: Long,
    // NEW FOR V2 COMPOSITE
    val name: String? = null,
    val conditionsJson: String? = null,
    val logicOperator: String? = null,
    val cooldownMinutes: Int = 60,
    val lastTriggeredAt: Long? = null,
) {
    fun toDomain(): Alert =
        Alert(
            id = id,
            coinId = coinId,
            coinSymbol = coinSymbol,
            targetPrice = targetPrice,
            direction = direction,
            isActive = isActive,
            isTriggered = isTriggered,
            createdAt = createdAt,
        )

    fun toCompositeDomain(gson: Gson): CompositeAlert {
        val conditionsType = object : TypeToken<List<AlertCondition>>() {}.type
        val conditions: List<AlertCondition> =
            conditionsJson?.let {
                gson.fromJson(it, conditionsType)
            } ?: emptyList()

        return CompositeAlert(
            id = id,
            name = name ?: "ALERT_$id",
            coinId = coinId,
            coinSymbol = coinSymbol,
            conditions = conditions.toImmutableList(),
            logicOperator = logicOperator?.let { AlertLogicOperator.valueOf(it) } ?: AlertLogicOperator.AND,
            isActive = isActive,
            isTriggered = isTriggered,
            cooldownMinutes = cooldownMinutes,
            lastTriggeredAt = lastTriggeredAt,
            createdAt = createdAt,
        )
    }

    companion object {
        fun fromDomain(alert: Alert) =
            AlertEntity(
                id = alert.id,
                coinId = alert.coinId,
                coinSymbol = alert.coinSymbol,
                targetPrice = alert.targetPrice,
                direction = alert.direction,
                isActive = alert.isActive,
                isTriggered = alert.isTriggered,
                createdAt = alert.createdAt,
            )

        fun fromComposite(
            alert: CompositeAlert,
            gson: Gson,
        ) = AlertEntity(
            id = alert.id,
            coinId = alert.coinId,
            coinSymbol = alert.coinSymbol,
            targetPrice = 0.0, // Standard price field not used in composite if strictly in conditions
            direction = AlertDirection.ABOVE,
            isActive = alert.isActive,
            isTriggered = alert.isTriggered,
            createdAt = alert.createdAt,
            name = alert.name,
            conditionsJson = gson.toJson(alert.conditions),
            logicOperator = alert.logicOperator.name,
            cooldownMinutes = alert.cooldownMinutes,
            lastTriggeredAt = alert.lastTriggeredAt,
        )
    }
}
