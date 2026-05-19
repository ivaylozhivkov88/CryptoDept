package com.cryptodept.data.api

import com.cryptodept.domain.repository.PriceProvider
import com.cryptodept.util.SymbolResolver
import javax.inject.Inject

class CoinbasePriceProvider @Inject constructor(
    private val coinbaseApi: CoinbaseApi,
    private val symbolResolver: SymbolResolver,
) : PriceProvider {

    override val providerName = "COINBASE"

    override suspend fun fetchPrice(coinGeckoId: String): Result<Double> {
        val symbol = symbolResolver.toCoinbaseSymbol(coinGeckoId)
            ?: return Result.failure(IllegalArgumentException("No Coinbase mapping for: $coinGeckoId"))
        return runCatching {
            coinbaseApi.getProductTicker(symbol).lastPrice
                ?: throw IllegalStateException("Price not found for $symbol")
        }
    }
}
