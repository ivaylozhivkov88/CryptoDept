package com.cryptodept.domain.repository

import com.cryptodept.domain.algo.TreemapItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HeatmapDataRepository
    @Inject
    constructor(
        private val cryptoRepository: CryptoRepository,
    ) {
        /**
         * Provides a list of top coins mapped to TreemapItems for the heatmap.
         */
        fun getHeatmapData(): Flow<List<TreemapItem>> =
            cryptoRepository.getAllCoinPrices().map { prices ->
                prices
                    .sortedByDescending { it.marketCap }
                    .take(50)
                    .map { price ->
                        TreemapItem(
                            symbol = price.symbol.uppercase(),
                            value = price.marketCap,
                            change24h = price.priceChangePercentage24h,
                        )
                    }
            }
    }
