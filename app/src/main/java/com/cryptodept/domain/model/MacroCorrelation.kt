package com.cryptodept.domain.model

data class MacroCorrelation(
    val asset: String, // "SPX", "GOLD", "DXY"
    val correlation: Double, // -1.0 to 1.0
    val strength: String, // "STRONG_POSITIVE", "INVERSE", etc.
    val description: String,
    val lastPrice: Double,
    val change24h: Double,
)

data class MacroDataPoint(
    val date: String,
    val price: Double,
)

data class MacroState(
    val btcHistory: List<MacroDataPoint>,
    val correlations: List<MacroCorrelation>,
)
