package com.cryptodept.data.api

import com.cryptodept.domain.repository.PriceProvider
import com.cryptodept.util.SymbolResolver
import javax.inject.Inject

class KrakenPriceProvider @Inject constructor(
    private val krakenApi: KrakenApi,
    private val symbolResolver: SymbolResolver,
) : PriceProvider {

    override val providerName = "KRAKEN"

    override suspend fun fetchPrice(coinGeckoId: String): Result<Double> {
        val symbol = symbolResolver.toKrakenSymbol(coinGeckoId)
            ?: return Result.failure(IllegalArgumentException("No Kraken mapping for: $coinGeckoId"))
        return runCatching {
            krakenApi.getTicker(symbol).result.values.first().lastPrice
                ?: throw IllegalStateException("Price not found for $symbol")
        }
    }
}
