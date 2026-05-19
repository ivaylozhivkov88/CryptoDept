package com.cryptodept.data.api

import com.cryptodept.domain.model.AggregatedPrice
import com.cryptodept.domain.repository.PriceProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MultiSourcePriceAggregator @Inject constructor(
    private val providers: Set<@JvmSuppressWildcards PriceProvider>,
) {
    private val lastPrices = mutableMapOf<String, MutableMap<String, Double>>()

    suspend fun fetchAggregatedPrice(
        coinId: String,
        binancePrice: Double?,
    ): AggregatedPrice = coroutineScope {
        val deferredPrices = providers.map { provider ->
            async {
                provider.fetchPrice(coinId).getOrNull()?.let { price ->
                    provider.providerName.lowercase() to price
                }
            }
        }

        val coinCache = lastPrices.getOrPut(coinId) { mutableMapOf() }
        deferredPrices.awaitAll().filterNotNull().forEach { (sourceName, price) ->
            coinCache[sourceName] = price
        }
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
}
