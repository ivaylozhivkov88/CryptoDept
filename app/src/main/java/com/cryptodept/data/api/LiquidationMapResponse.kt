package com.cryptodept.data.api

data class LiquidationMapResponse(
    val code: Int,
    val data: LiquidationMapData?
)

data class LiquidationMapData(
    val liqList: List<LiquidationLevel>
)

data class LiquidationLevel(
    val price: Double,       // price level
    val liqSize: Double,     // USD value of liquidations at this level
    val direction: String    // "long" or "short"
)
