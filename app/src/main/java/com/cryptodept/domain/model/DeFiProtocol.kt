package com.cryptodept.domain.model

data class DeFiProtocol(
    val id: String,
    val name: String,
    val symbol: String,
    val url: String,
    val description: String,
    val logo: String,
    val tvl: Double,
    val tvlChange1h: Double,
    val tvlChange1d: Double,
    val tvlChange7d: Double,
    val chain: String,
    val category: String,
)

data class DeFiYieldOpportunity(
    val protocol: String,
    val symbol: String,
    val tvl: Double,
    val apy: Double,
    val chain: String,
    val isAuditVerified: Boolean = true,
)
