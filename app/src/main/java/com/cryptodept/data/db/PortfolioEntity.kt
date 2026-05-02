package com.cryptodept.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cryptodept.domain.model.PortfolioEntry

@Entity(tableName = "portfolio")
data class PortfolioEntity(
    @PrimaryKey val id: String,
    val coinId: String,
    val symbol: String,
    val quantity: Double,
    val averageEntryPrice: Double,
    val addedAt: Long
) {
    fun toDomain() = PortfolioEntry(
        id = id,
        coinId = coinId,
        symbol = symbol,
        quantity = quantity,
        averageEntryPrice = averageEntryPrice,
        addedAt = addedAt
    )

    companion object {
        fun fromDomain(entry: PortfolioEntry) = PortfolioEntity(
            id = entry.id,
            coinId = entry.coinId,
            symbol = entry.symbol,
            quantity = entry.quantity,
            averageEntryPrice = entry.averageEntryPrice,
            addedAt = entry.addedAt
        )
    }
}
