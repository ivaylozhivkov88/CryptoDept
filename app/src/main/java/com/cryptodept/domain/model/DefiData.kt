package com.cryptodept.domain.model

data class LpSimulationResult(
    val initialValue: Double,
    val finalValue: Double,
    val impermanentLoss: Double,
    val gainWithYield: Double,
    val netProfit: Double
)
