package com.cryptodept.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cryptodept.domain.model.Coin
import com.cryptodept.domain.model.CoinPrice
import kotlinx.collections.immutable.persistentListOf

@Entity(tableName = "coins")
data class CoinEntity(
    @PrimaryKey val id: String,
    val symbol: String,
    val name: String,
    val isTracked: Boolean,
    val currentPrice: Double = 0.0,
    val priceChange24h: Double = 0.0,
    val priceChangePercentage24h: Double = 0.0,
    val marketCap: Double = 0.0,
    val totalVolume: Double = 0.0,
    val high24h: Double = 0.0,
    val low24h: Double = 0.0,
    val lastUpdated: Long = 0L,
    val rank: Int = 0,
    val sourcesCount: Int = 1,
    val maxDeviation: Double = 0.0,
) {
    fun toDomain() =
        Coin(
            id = id,
            symbol = symbol.uppercase(), // CoinGecko връща "btc" → ние показваме "BTC"
            name = name,
            isTracked = isTracked,
        )

    fun toDomainPrice() =
        CoinPrice(
            id = id,
            symbol = symbol.uppercase(), // Fix: CoinGecko връща lowercase ("btc", "eth", "xrp")
            name = name,
            currentPrice = currentPrice,
            priceChange24h = priceChange24h,
            priceChangePercentage24h = priceChangePercentage24h,
            marketCap = marketCap,
            totalVolume = totalVolume,
            high24h = high24h,
            low24h = low24h,
            lastUpdated = lastUpdated,
            isTracked = isTracked,
            sparkline = persistentListOf<Double>(),
            sourcesCount = sourcesCount,
            maxDeviation = maxDeviation,
        )

    companion object {
        fun fromDomain(coin: Coin) =
            CoinEntity(
                id = coin.id,
                symbol = coin.symbol,
                name = coin.name,
                isTracked = coin.isTracked,
            )
    }
}
