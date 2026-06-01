package com.cryptodept.data.repository

import com.cryptodept.data.api.CoinGeckoApi
import com.cryptodept.domain.model.OHLCData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoinGeckoPriceSource @Inject constructor(
    private val api: CoinGeckoApi,
    private val symbolResolver: com.cryptodept.util.SymbolResolver
) : PriceDataSource {
    private fun normalizeDays(days: Int): String {
        return when {
            days <= 1 -> "1"
            days <= 7 -> "7"
            days <= 14 -> "14"
            days <= 30 -> "30"
            days <= 90 -> "90"
            days <= 180 -> "180"
            days <= 365 -> "365"
            else -> "max"
        }
    }

    private fun resolveCoinGeckoId(id: String): String {
        return when (id.uppercase()) {
            "BTC" -> "bitcoin"
            "ETH" -> "ethereum"
            "BNB" -> "binancecoin"
            "SOL" -> "solana"
            "XRP" -> "ripple"
            "DOGE" -> "dogecoin"
            "ADA" -> "cardano"
            "TRX" -> "tron"
            "MATIC" -> "matic-network"
            "DOT" -> "polkadot"
            "LTC" -> "litecoin"
            "AVAX" -> "avalanche-2"
            "LINK" -> "chainlink"
            "UNI" -> "uniswap"
            "ATOM" -> "cosmos"
            else -> id.lowercase()
        }
    }

    override suspend fun getOHLCData(coinId: String, days: Int): List<OHLCData> {
        return try {
            val normalizedId = symbolResolver.toCoinGeckoId(coinId)
            val response = api.getCoinOHLC(normalizedId, "usd", normalizeDays(days))
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
            val normalizedId = symbolResolver.toCoinGeckoId(coinId)
            val response = api.getSimplePrice(normalizedId, "usd")
            response[normalizedId]?.get("usd")
        } catch (e: Exception) {
            null
        }
    }
}
