package com.cryptodept.data.repository

import com.cryptodept.domain.model.OHLCData

interface PriceDataSource {
    suspend fun getOHLCData(coinId: String, days: Int): List<OHLCData>
    suspend fun getCurrentPrice(coinId: String): Double?
}
