package com.cryptodept.data.repository

import com.cryptodept.data.api.BinanceFuturesApi
import com.cryptodept.domain.model.OHLCData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BinancePriceSource @Inject constructor(
    private val api: BinanceFuturesApi,
    private val symbolResolver: com.cryptodept.util.SymbolResolver
) : PriceDataSource {
    override suspend fun getOHLCData(coinId: String, days: Int): List<OHLCData> {
        val symbol = symbolResolver.toBinanceSymbol(coinId)
        
        return try {
            val interval = if (days <= 2) "1h" else "1d"
            val limit = if (days <= 2) days * 24 else days
            
            api.getKlines(symbol, interval, limit).map { item ->
                OHLCData(
                    timestamp = item[0].toString().toDouble().toLong(),
                    open = item[1].toString().toDouble(),
                    high = item[2].toString().toDouble(),
                    low = item[3].toString().toDouble(),
                    close = item[4].toString().toDouble(),
                    volume = item[5].toString().toDouble()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getCurrentPrice(coinId: String): Double? {
        // Implementation for single price from Binance if needed
        return null
    }
}
