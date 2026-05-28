package com.cryptodept.data.api

import com.google.gson.annotations.SerializedName

data class LiquidationMapResponse(
    @SerializedName("code") val code: String,
    @SerializedName("data") val data: LiquidationMapData?
)

data class LiquidationMapData(
    @SerializedName("chartData") val chartData: HeatmapDataDto?,
    @SerializedName("list") val liqList: List<LiquidationLevel>? = emptyList()
)

data class LiquidationLevel(
    @SerializedName("price") val price: Double,
    @SerializedName("vol") val liqSize: Double,
    @SerializedName("side") val direction: String // "buy" or "sell"
)
