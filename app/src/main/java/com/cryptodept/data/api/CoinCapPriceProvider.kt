package com.cryptodept.data.api

import com.cryptodept.domain.repository.PriceProvider
import com.cryptodept.util.SymbolResolver
import javax.inject.Inject

class CoinCapPriceProvider @Inject constructor(
    private val coinCapApi: CoinCapApi,
    private val symbolResolver: SymbolResolver,
) : PriceProvider {

    override val providerName = "COINCAP"

    override suspend fun fetchPrice(coinGeckoId: String): Result<Double> {
        val assetId = symbolResolver.toCoinCapId(coinGeckoId)
        return runCatching {
            coinCapApi.getAsset(assetId).data.lastPrice
                ?: throw IllegalStateException("Price not found for $assetId")
        }
    }
}
