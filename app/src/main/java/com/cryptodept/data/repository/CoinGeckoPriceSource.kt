package com.cryptodept.data.repository

import com.cryptodept.data.api.CoinGeckoApi
import com.cryptodept.domain.model.OHLCData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoinGeckoPriceSource @Inject constructor(
    private val api: CoinGeckoApi
) : PriceDataSource {
    override suspend fun getOHLCData(coinId: String, days: Int): List<OHLCData> {
        return try {
            val response = api.getCoinOHLC(coinId, "usd", days.toString())
            response.mapNotNull { item ->
                if (item.size >= 5) {
                    OHLCData(
                        timestamp = item[0].toLong(),
                        open = item[1],
                        high = item[2],
                        low = item[3],
                        close = item[4],
                        volume = 0.0
                    )
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getCurrentPrice(coinId: String): Double? {
        return try {
            val response = api.getSimplePrice(coinId, "usd")
            response[coinId]?.get("usd")
        } catch (e: Exception) {
            null
        }
    }
}
