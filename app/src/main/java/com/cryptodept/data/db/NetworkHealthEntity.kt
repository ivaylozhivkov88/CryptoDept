package com.cryptodept.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cryptodept.domain.model.NetworkHealth

@Entity(tableName = "network_health")
data class NetworkHealthEntity(
    @PrimaryKey val id: Int = 0, // Single row for latest state
    val btcHashrate: String,
    val btcMempool: String,
    val ethGas: String,
    val fearGreedIndex: Int,
    val fearGreedLabel: String,
    val socialPulse: Int,
    val socialPulseLabel: String,
    val lastUpdated: Long
) {
    fun toDomain() = NetworkHealth(
        btcHashrate = btcHashrate,
        btcMempool = btcMempool,
        ethGas = ethGas,
        fearGreedIndex = fearGreedIndex,
        fearGreedLabel = fearGreedLabel,
        socialPulse = socialPulse,
        socialPulseLabel = socialPulseLabel,
        lastUpdated = lastUpdated
    )

    companion object {
        fun fromDomain(health: NetworkHealth) = NetworkHealthEntity(
            btcHashrate = health.btcHashrate,
            btcMempool = health.btcMempool,
            ethGas = health.ethGas,
            fearGreedIndex = health.fearGreedIndex,
            fearGreedLabel = health.fearGreedLabel,
            socialPulse = health.socialPulse,
            socialPulseLabel = health.socialPulseLabel,
            lastUpdated = health.lastUpdated
        )
    }
}
