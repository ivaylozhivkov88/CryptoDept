package com.cryptodept.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cryptodept.domain.model.GlobalMarketData

@Entity(tableName = "global_market_data")
data class GlobalMarketEntity(
    @PrimaryKey val id: Int = 0, // Singleton record
    val activeCoins: Int,
    val totalMarketCap: Double,
    val totalVolume: Double,
    val marketCapChangePercentage24h: Double,
    val btcDominance: Double,
    val ethDominance: Double,
    val lastUpdated: Long
) {
    fun toDomain() = GlobalMarketData(
        activeCoins = activeCoins,
        totalMarketCap = totalMarketCap,
        totalVolume = totalVolume,
        marketCapChangePercentage24h = marketCapChangePercentage24h,
        btcDominance = btcDominance,
        ethDominance = ethDominance
    )

    companion object {
        fun fromDomain(data: GlobalMarketData) = GlobalMarketEntity(
            activeCoins = data.activeCoins,
            totalMarketCap = data.totalMarketCap,
            totalVolume = data.totalVolume,
            marketCapChangePercentage24h = data.marketCapChangePercentage24h,
            btcDominance = data.btcDominance,
            ethDominance = data.ethDominance,
            lastUpdated = System.currentTimeMillis()
        )
    }
}
