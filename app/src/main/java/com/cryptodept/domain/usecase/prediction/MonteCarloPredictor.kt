package com.cryptodept.domain.usecase.prediction

import com.cryptodept.domain.model.Direction
import com.cryptodept.domain.model.ModelVote
import com.cryptodept.domain.model.PredictionModel
import com.cryptodept.domain.model.PriceDistribution
import java.util.*
import javax.inject.Inject
import kotlin.math.*

/**
 * Вероятностен симулатор чрез Geometric Brownian Motion (GBM).
 * Изпълнява 1000 симулации на бъдещата цена, за да генерира
 * разпределение на риска и перцентили.
 */
class MonteCarloPredictor
    @Inject
    constructor() {
        private val SIMULATIONS = 1000
        private val random = Random()

        /**
         * Симулира 1000 ценови пътеки, за да изчисли вероятностното разпределение.
         */
        fun simulate(
            closes: List<Double>,
            periodsAhead: Int,
        ): Pair<ModelVote, PriceDistribution> {
            // Изискваме поне 10 точки за изчисляване на волатилност (sigma)
            if (closes.size < 10) return neutralResult()

            val currentPrice = closes.last()

            // 1. Изчисляваме логаритмичната доходност (Log Returns) за статистически модел
            val returns = closes.zipWithNext { a, b -> ln(b / a) }
            val mu = returns.average()
            val sigma = returns.stdDev()

            // Drift компонента (средното движение, коригирано с волатилността)
            // Ако волатилността е 0, drift е 0
            val drift = if (sigma > 0) mu - (sigma.pow(2.0) / 2.0) else 0.0

            // 2. Изпълняваме симулациите
            val finalPrices = mutableListOf<Double>()

            repeat(SIMULATIONS) {
                var simulatedPrice = currentPrice
                if (sigma > 0) {
                    repeat(periodsAhead) {
                        val shock = sigma * random.nextGaussian()
                        simulatedPrice *= exp(drift + shock)
                    }
                }
                finalPrices.add(simulatedPrice)
            }

            // Сортираме резултатите за извличане на перцентили
            finalPrices.sort()

            val avg = finalPrices.average()
            val std = finalPrices.stdDev()

            // 3. Изчисляваме перцентилите за терминалния UI
            val distribution =
                PriceDistribution(
                    percentile10 = finalPrices[(SIMULATIONS * 0.10).toInt()],
                    percentile25 = finalPrices[(SIMULATIONS * 0.25).toInt()],
                    percentile50 = finalPrices[(SIMULATIONS * 0.50).toInt()], // Медиана
                    percentile75 = finalPrices[(SIMULATIONS * 0.75).toInt()],
                    percentile90 = finalPrices[(SIMULATIONS * 0.90).toInt()],
                    expectedValue = avg,
                    standardDeviation = std,
                    skewness = calculateSkewness(finalPrices, avg, std),
                )

            // 4. Определяме гласа на модела (Vote)
            val bullishSimulations = finalPrices.count { it > currentPrice }
            val bullishRatio = bullishSimulations.toFloat() / SIMULATIONS

            val vote =
                ModelVote(
                    model = PredictionModel.MONTE_CARLO,
                    direction =
                        when {
                            bullishRatio > 0.65f -> Direction.STRONG_UP
                            bullishRatio > 0.55f -> Direction.UP
                            bullishRatio < 0.35f -> Direction.STRONG_DOWN
                            bullishRatio < 0.45f -> Direction.DOWN
                            else -> Direction.SIDEWAYS
                        },
                    targetPrice = distribution.percentile50,
                    confidence = (abs(bullishRatio - 0.5f) * 2f).coerceIn(0f, 1f),
                    weight = PredictionModel.MONTE_CARLO.baseWeight,
                )

            return Pair(vote, distribution)
        }

        private fun calculateSkewness(
            data: List<Double>,
            mean: Double,
            stdDev: Double,
        ): Double {
            if (stdDev == 0.0) return 0.0
            val n = data.size.toDouble()
            val m3 = data.sumOf { (it - mean).pow(3.0) } / n
            return m3 / stdDev.pow(3.0)
        }

        private fun List<Double>.stdDev(): Double {
            if (this.isEmpty()) return 0.0
            val mean = this.average()
            return sqrt(this.map { (it - mean).pow(2) }.average())
        }

        private fun neutralResult(): Pair<ModelVote, PriceDistribution> {
            val emptyDist =
                PriceDistribution(
                    percentile10 = 0.0,
                    percentile25 = 0.0,
                    percentile50 = 0.0,
                    percentile75 = 0.0,
                    percentile90 = 0.0,
                    expectedValue = 0.0,
                    standardDeviation = 0.0,
                    skewness = 0.0,
                )
            val vote =
                ModelVote(
                    model = PredictionModel.MONTE_CARLO,
                    direction = Direction.SIDEWAYS,
                    targetPrice = 0.0,
                    confidence = 0f,
                    weight = PredictionModel.MONTE_CARLO.baseWeight,
                )
            return Pair(vote, emptyDist)
        }
    }
