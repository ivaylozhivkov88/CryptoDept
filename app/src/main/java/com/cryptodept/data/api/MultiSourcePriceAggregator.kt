package com.cryptodept.data.api

import com.cryptodept.domain.model.AggregatedPrice
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MultiSourcePriceAggregator
    @Inject
    constructor(
        private val krakenApi: KrakenApi,
        private val coinbaseApi: CoinbaseApi,
        private val coinCapApi: CoinCapApi,
        private val coinPaprikaApi: CoinPaprikaApi,
    ) {
        // Cache for WS updates
        private val lastPrices = mutableMapOf<String, MutableMap<String, Double>>()

        suspend fun fetchAggregatedPrice(
            coinId: String,
            binancePrice: Double?,
        ): AggregatedPrice =
            coroutineScope {
                val krakenSymbol = mapToKraken(coinId)
                val coinbaseSymbol = mapToCoinbase(coinId)
                val coinCapId = mapToCoinCap(coinId)
                val paprikaId = mapToPaprika(coinId)

                val deferredKraken =
                    async {
                        runCatching {
                            krakenApi
                                .getTicker(krakenSymbol)
                                .result.values
                                .first()
                                .lastPrice
                        }.getOrNull()
                    }
                val deferredCoinbase = async { runCatching { coinbaseApi.getProductTicker(coinbaseSymbol).lastPrice }.getOrNull() }
                val deferredCoinCap = async { runCatching { coinCapApi.getAsset(coinCapId).data.lastPrice }.getOrNull() }
                val deferredPaprika = async { runCatching { coinPaprikaApi.getTicker(paprikaId).lastPrice }.getOrNull() }

                val results = awaitAll(deferredKraken, deferredCoinbase, deferredCoinCap, deferredPaprika)

                // Update Cache
                val coinCache = lastPrices.getOrPut(coinId) { mutableMapOf() }
                results[0]?.let { coinCache["kraken"] = it }
                results[1]?.let { coinCache["coinbase"] = it }
                results[2]?.let { coinCache["coincap"] = it }
                results[3]?.let { coinCache["paprika"] = it }
                binancePrice?.let { coinCache["binance"] = it }

                calculateFromCache(coinId, binancePrice)
            }

        fun updatePriceFromWS(
            coinId: String,
            source: String,
            price: Double,
        ): AggregatedPrice {
            val coinCache = lastPrices.getOrPut(coinId) { mutableMapOf() }
            coinCache[source] = price
            return calculateFromCache(coinId, coinCache["binance"])
        }

        private fun calculateFromCache(
            coinId: String,
            currentBinance: Double?,
        ): AggregatedPrice {
            val coinCache = lastPrices[coinId] ?: mutableMapOf()
            val validPrices = coinCache.values.filter { it > 0 }
            val median = calculateMedian(validPrices)

            val maxPrice = validPrices.maxOrNull() ?: median
            val minPrice = validPrices.minOrNull() ?: median
            val deviation = if (median > 0) ((maxPrice - minPrice) / median) * 100 else 0.0

            return AggregatedPrice(
                coinId = coinId,
                binancePrice = currentBinance,
                krakenPrice = coinCache["kraken"],
                coinbasePrice = coinCache["coinbase"],
                coincapPrice = coinCache["coincap"],
                coinpaprikaPrice = coinCache["paprika"],
                consensusPrice = median,
                maxDeviationPercent = deviation,
                isReliable = deviation < 0.5 && validPrices.size >= 3,
                sourcesCount = validPrices.size,
            )
        }

        private fun calculateMedian(list: List<Double>): Double {
            if (list.isEmpty()) return 0.0
            val sorted = list.sorted()
            return if (sorted.size % 2 == 0) {
                (sorted[sorted.size / 2] + sorted[sorted.size / 2 - 1]) / 2
            } else {
                sorted[sorted.size / 2]
            }
        }

        private fun mapToKraken(id: String) =
            when (id) {
                "bitcoin" -> "XBTUSD"
                "ethereum" -> "ETHUSD"
                "ripple" -> "XRPUSD"
                "solana" -> "SOLUSD"
                "cardano" -> "ADAUSD"
                "polkadot" -> "DOTUSD"
                "dogecoin" -> "DOGEUSD"
                "chainlink" -> "LINKUSD"
                "shiba-inu" -> "SHIBUSD"
                "litecoin" -> "LTCUSD"
                "avalanche-2" -> "AVAXUSD"
                "tron" -> "TRXUSD"
                "matic-network" -> "MATICUSD"
                "stellar" -> "XLMUSD"
                "cosmos" -> "ATOMUSD"
                else -> "${id.uppercase()}USD"
            }

        private fun mapToCoinbase(id: String) =
            when (id) {
                "bitcoin" -> "BTC-USD"
                "ethereum" -> "ETH-USD"
                "ripple" -> "XRP-USD"
                "solana" -> "SOL-USD"
                "cardano" -> "ADA-USD"
                "polkadot" -> "DOT-USD"
                "dogecoin" -> "DOGE-USD"
                "chainlink" -> "LINK-USD"
                "shiba-inu" -> "SHIB-USD"
                "litecoin" -> "LTC-USD"
                "avalanche-2" -> "AVAX-USD"
                "tron" -> "TRX-USD"
                "matic-network" -> "MATIC-USD"
                "stellar" -> "XLM-USD"
                "cosmos" -> "ATOM-USD"
                else -> "${id.uppercase()}-USD"
            }

        private fun mapToCoinCap(id: String) =
            when (id) {
                "bitcoin" -> "bitcoin"
                "ethereum" -> "ethereum"
                "ripple" -> "xrp"
                "solana" -> "solana"
                "cardano" -> "cardano"
                "polkadot" -> "polkadot"
                "dogecoin" -> "dogecoin"
                "chainlink" -> "chainlink"
                "shiba-inu" -> "shiba-inu"
                "litecoin" -> "litecoin"
                "avalanche-2" -> "avalanche"
                "tron" -> "tron"
                "matic-network" -> "polygon"
                "stellar" -> "stellar"
                "cosmos" -> "cosmos"
                else -> id.lowercase()
            }

        private fun mapToPaprika(id: String) =
            when (id) {
                "bitcoin" -> "btc-bitcoin"
                "ethereum" -> "eth-ethereum"
                "ripple" -> "xrp-ripple"
                "solana" -> "sol-solana"
                "cardano" -> "ada-cardano"
                "polkadot" -> "dot-polkadot"
                "dogecoin" -> "doge-dogecoin"
                "chainlink" -> "link-chainlink"
                "shiba-inu" -> "shib-shiba-inu"
                "litecoin" -> "ltc-litecoin"
                "avalanche-2" -> "avax-avalanche"
                "tron" -> "trx-tron"
                "matic-network" -> "matic-polygon"
                "stellar" -> "xlm-stellar"
                "cosmos" -> "atom-cosmos"
                else -> "$id-$id"
            }
    }
