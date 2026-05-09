package com.cryptodept.domain.model

data class DashboardData(
    val prices: List<CoinPrice>,
    val networkHealth: NetworkHealth?,
    val aiSummary: String,
    val isAdmin: Boolean,
)
