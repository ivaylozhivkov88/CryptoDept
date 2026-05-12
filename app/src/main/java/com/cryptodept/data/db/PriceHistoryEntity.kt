package com.cryptodept.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "price_history",
    indices = [Index(value = ["coinId", "timestamp"], unique = true)],
)
data class PriceHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val coinId: String,
    val timestamp: Long,
    val price: Double,
    val volume: Double,
)
