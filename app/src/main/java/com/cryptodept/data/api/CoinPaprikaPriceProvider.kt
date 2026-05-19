package com.cryptodept.data.api

import com.cryptodept.domain.repository.PriceProvider
import com.cryptodept.util.SymbolResolver
import javax.inject.Inject

class CoinPaprikaPriceProvider @Inject constructor(
    private val coinPaprikaApi: CoinPaprikaApi,
    private val symbolResolver: SymbolResolver,
) : PriceProvider {

    override val providerName = "PAPRIKA"

    override suspend fun fetchPrice(coinGeckoId: String): Result<Double> {
        val paprikaId = symbolResolver.toCoinPaprikaId(coinGeckoId)
            ?: return Result.failure(IllegalArgumentException("No Paprika mapping for: $coinGeckoId"))
        return runCatching {
            coinPaprikaApi.getTicker(paprikaId).lastPrice
                ?: throw IllegalStateException("Price not found for $paprikaId")
        }
    }
}
