package com.cryptodept.domain.usecase

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class CorrelationEngine
    @Inject
    constructor() {
        /**
         * Calculates Pearson Correlation Coefficient between two lists of prices.
         * Prices must have the same length.
         */
        fun calculatePearson(
            prices1: List<Double>,
            prices2: List<Double>,
        ): Double {
            if (prices1.size != prices2.size || prices1.isEmpty()) return 0.0

            val n = prices1.size
            val mean1 = prices1.average()
            val mean2 = prices2.average()

            var numerator = 0.0
            var sumSqDiff1 = 0.0
            var sumSqDiff2 = 0.0

            for (i in 0 until n) {
                val diff1 = prices1[i] - mean1
                val diff2 = prices2[i] - mean2
                numerator += diff1 * diff2
                sumSqDiff1 += diff1 * diff1
                sumSqDiff2 += diff2 * diff2
            }

            val denominator = sqrt(sumSqDiff1 * sumSqDiff2)
            if (denominator == 0.0) return 0.0

            return (numerator / denominator).coerceIn(-1.0, 1.0)
        }

        /**
         * Normalizes price series to ensure they are comparable if lengths differ slightly
         * (though for correlation they should be matched by timestamp).
         */
        fun alignPrices(
            data1: List<Pair<Long, Double>>,
            data2: List<Pair<Long, Double>>,
        ): Pair<List<Double>, List<Double>> {
            val map2 = data2.toMap()
            val aligned1 = mutableListOf<Double>()
            val aligned2 = mutableListOf<Double>()

            for ((ts, price) in data1) {
                val price2 = map2[ts]
                if (price2 != null) {
                    aligned1.add(price)
                    aligned2.add(price2)
                }
            }
            return Pair(aligned1, aligned2)
        }
    }
