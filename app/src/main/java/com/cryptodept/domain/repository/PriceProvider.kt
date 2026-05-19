package com.cryptodept.domain.repository

/**
 * Strategy interface for a single price data source.
 * Each exchange implementation knows only about itself.
 * MultiSourcePriceAggregator does not know which exchanges exist.
 */
interface PriceProvider {
    /** Human-readable name used for logging and cache keys. */
    val providerName: String

    /**
     * Fetch the current price for the given CoinGecko ID.
     * @return Result.success(price) or Result.failure(exception) — never throws.
     */
    suspend fun fetchPrice(coinGeckoId: String): Result<Double>
}
