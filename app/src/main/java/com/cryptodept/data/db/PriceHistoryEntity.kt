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
    val open: Double = 0.0,
    val high: Double = 0.0,
    val low: Double = 0.0,
    val price: Double, // Close price
    val volume: Double,
)
