package com.cryptodept.domain.usecase.prediction

import com.cryptodept.domain.model.Direction
import com.cryptodept.domain.model.ModelVote
import com.cryptodept.domain.model.PredictionModel
import javax.inject.Inject
import kotlin.math.*

/**
 * Най-простият статистически модел.
 * Използва метода на най-малките квадрати, за да начертае права линия през ценовите точки.
 */
class LinearRegressionPredictor
    @Inject
    constructor() {
        fun predict(
            prices: List<Double>,
            periodsAhead: Int,
        ): ModelVote {
            // Изискваме поне 10 точки за смислена регресия
            if (prices.size < 10) return neutralVote()

            val n = prices.size
            val x = (0 until n).map { it.toDouble() }
            val y = prices

            val sumX = x.sum()
            val sumY = y.sum()
            val sumXY = x.zip(y).sumOf { it.first * it.second }
            val sumX2 = x.sumOf { it * it }

            val denominator = n * sumX2 - sumX * sumX
            if (denominator == 0.0) return neutralVote()

            // Изчисляване на наклона (slope) и пресечната точка (intercept)
            val slope = (n * sumXY - sumX * sumY) / denominator
            val intercept = (sumY - slope * sumX) / n

            // Прогнозирана цена след X периода
            val futureX = (n - 1) + periodsAhead
            val predictedPrice = slope * futureX + intercept
            val currentPrice = prices.last()

            // Изчисляване на R² (Confidence на модела)
            val rSquared = calculateRSquared(x, y, slope, intercept)

            val direction =
                when {
                    predictedPrice > currentPrice * 1.005 -> Direction.UP
                    predictedPrice < currentPrice * 0.995 -> Direction.DOWN
                    else -> Direction.SIDEWAYS
                }

            return ModelVote(
                model = PredictionModel.LINEAR_REGRESSION,
                direction = direction,
                targetPrice = predictedPrice,
                confidence = rSquared.toFloat().coerceIn(0f, 1f),
                weight = PredictionModel.LINEAR_REGRESSION.baseWeight,
            )
        }

        /**
         * Изчислява коефициента на детерминация (R²).
         * Дава информация колко надеждна е линейната зависимост.
         */
        private fun calculateRSquared(
            x: List<Double>,
            y: List<Double>,
            slope: Double,
            intercept: Double,
        ): Double {
            val yMean = y.average()
            val ssTot = y.sumOf { (it - yMean).pow(2) }
            val ssRes =
                x.zip(y).sumOf { (xi, yi) ->
                    val prediction = slope * xi + intercept
                    (yi - prediction).pow(2)
                }

            if (ssTot == 0.0) return 0.0
            return 1.0 - (ssRes / ssTot)
        }

        private fun neutralVote() =
            ModelVote(
                model = PredictionModel.LINEAR_REGRESSION,
                direction = Direction.SIDEWAYS,
                targetPrice = 0.0,
                confidence = 0f,
                weight = PredictionModel.LINEAR_REGRESSION.baseWeight,
            )
    }
