package com.cryptodept.domain.usecase.prediction

import com.cryptodept.domain.model.Direction
import com.cryptodept.domain.model.ModelVote
import com.cryptodept.domain.model.PredictionModel
import com.cryptodept.util.AppConstants
import javax.inject.Inject

class ElliottWavePredictor
    @Inject
    constructor() {
        data class PivotPoint(
            val price: Double,
            val index: Int,
            val isHigh: Boolean,
        )

        fun predict(closes: List<Double>): ModelVote {
            // Require minimum candles for adequate wave analysis
            if (closes.size < AppConstants.Prediction.MIN_DATA_POINTS) return neutralVote()

            val pivots = findPivots(closes)
            // For basic 5-wave structure recognition we need at least 5 pivots
            if (pivots.size < AppConstants.Prediction.ELLIOTT_WAVE_MIN_PIVOTS) return neutralVote()

            val lastPivots = pivots.takeLast(AppConstants.Prediction.ELLIOTT_WAVE_MIN_PIVOTS)
            val direction = analyzeWaveStructure(lastPivots)

            val currentPrice = closes.last()

            // Calculate target price based on basic multipliers
            val targetPrice =
                when (direction) {
                    Direction.STRONG_UP, Direction.UP -> currentPrice * AppConstants.Prediction.BULLISH_TARGET_MULTIPLIER
                    Direction.STRONG_DOWN, Direction.DOWN -> currentPrice * AppConstants.Prediction.BEARISH_TARGET_MULTIPLIER
                    else -> currentPrice
                }

            return ModelVote(
                model = PredictionModel.ELLIOTT_WAVE,
                direction = direction,
                targetPrice = targetPrice,
                confidence = AppConstants.Prediction.ELLIOTT_WAVE_CONFIDENCE,
                weight = PredictionModel.ELLIOTT_WAVE.baseWeight,
            )
        }

        private fun findPivots(prices: List<Double>): List<PivotPoint> {
            val pivots = mutableListOf<PivotPoint>()
            val window = AppConstants.Prediction.ELLIOTT_WAVE_WINDOW // Window for local extremum

            for (i in window until prices.size - window) {
                val current = prices[i]
                val range = prices.subList(i - window, i + window + 1)

                if (current == range.maxOrNull()) {
                    pivots.add(PivotPoint(current, i, true))
                } else if (current == range.minOrNull()) {
                    pivots.add(PivotPoint(current, i, false))
                }
            }
            return pivots
        }

        private fun analyzeWaveStructure(pivots: List<PivotPoint>): Direction {
            // Check for higher highs and higher lows (Bullish Trend)
            val isBullish =
                pivots.zipWithNext().all { (a, b) ->
                    when {
                        a.isHigh && b.isHigh -> b.price > a.price
                        !a.isHigh && !b.isHigh -> b.price > a.price
                        else -> true
                    }
                }

            // Check for lower highs and lower lows (Bearish Trend)
            val isBearish =
                pivots.zipWithNext().all { (a, b) ->
                    when {
                        a.isHigh && b.isHigh -> b.price < a.price
                        !a.isHigh && !b.isHigh -> b.price < a.price
                        else -> true
                    }
                }

            return when {
                isBullish -> Direction.STRONG_UP
                isBearish -> Direction.STRONG_DOWN
                else -> Direction.SIDEWAYS
            }
        }

        private fun neutralVote() =
            ModelVote(
                model = PredictionModel.ELLIOTT_WAVE,
                direction = Direction.SIDEWAYS,
                targetPrice = 0.0,
                confidence = 0f,
                weight = PredictionModel.ELLIOTT_WAVE.baseWeight,
            )
    }
