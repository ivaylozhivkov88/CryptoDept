package com.cryptodept.domain.usecase.prediction

import com.cryptodept.domain.model.Direction
import com.cryptodept.domain.model.ModelVote
import com.cryptodept.domain.model.PredictionModel
import javax.inject.Inject
import kotlin.math.*

/**
 * Математиката на тренд персистирането.
 * Hurst Exponent (H) измерва паметта на времевия ред:
 * - H > 0.55 -> Trend Persisting (пазарът има памет, следвай тренда)
 * - H = 0.50 -> Random Walk (хаотично движение, Brownian motion)
 * - H < 0.45 -> Mean Reverting (пазарът се стреми да се върне към средното)
 */
class HurstExponentCalculator
    @Inject
    constructor() {
        /**
         * Изчислява Hurst Exponent чрез R/S анализ (Rescaled Range).
         */
        fun calculate(prices: List<Double>): Float {
            // Изискваме поне 32 точки за смислен статистически анализ
            if (prices.size < 32) return 0.5f

            // 1. Изчисляваме логаритмичната доходност (Log Returns)
            val returns = prices.zipWithNext { a, b -> ln(b / a) }
            val n = returns.size

            // Използваме стандартни размери на прозорци за регресията (логаритмична скала)
            val windowSizes = listOf(8, 16, 32, 64, n).filter { it <= n }
            val logRS = mutableListOf<Double>()
            val logN = mutableListOf<Double>()

            for (size in windowSizes) {
                val rsValue = calculateRS(returns.takeLast(size))
                if (rsValue > 0) {
                    logRS.add(ln(rsValue))
                    logN.add(ln(size.toDouble()))
                }
            }

            // Hurst exponent е наклонът (slope) на регресията между log(N) и log(R/S)
            if (logN.size < 2) return 0.5f

            return calculateSlope(logN, logRS).toFloat().coerceIn(0f, 1f)
        }

        /**
         * Изчислява Rescaled Range (R/S) за даден сегмент от данни.
         */
        private fun calculateRS(data: List<Double>): Double {
            if (data.isEmpty()) return 0.0

            val mean = data.average()
            val deviations = data.map { it - mean }

            // Cumulative deviation (Z) - отклонение от средното във времето
            var currentSum = 0.0
            val z =
                deviations.map {
                    currentSum += it
                    currentSum
                }

            // Range (R) - разликата между максималното и минималното натрупано отклонение
            val range = (z.maxOrNull() ?: 0.0) - (z.minOrNull() ?: 0.0)

            // Standard Deviation (S)
            val stdDev = sqrt(data.map { (it - mean).pow(2) }.average())

            return if (stdDev > 0) range / stdDev else 0.0
        }

        /**
         * Проста линейна регресия за намиране на наклона.
         */
        private fun calculateSlope(
            x: List<Double>,
            y: List<Double>,
        ): Double {
            val n = x.size.toDouble()
            val sumX = x.sum()
            val sumY = y.sum()
            val sumXY = x.zip(y).sumOf { it.first * it.second }
            val sumX2 = x.sumOf { it * it }

            val denominator = (n * sumX2 - sumX * sumX)
            if (denominator == 0.0) return 0.5

            return (n * sumXY - sumX * sumY) / denominator
        }

        /**
         * Превръща стойноста на Hurst в конкретен вот за ансамбъла.
         */
        fun interpret(
            hurst: Float,
            currentTrend: Direction,
        ): ModelVote =
            when {
                // H > 0.55: Трендът е стабилен (Positive Autocorrelation)
                hurst > 0.55f ->
                    ModelVote(
                        model = PredictionModel.HURST_EXPONENT,
                        direction = currentTrend,
                        targetPrice = 0.0, // Hurst не дава цена, а посока/стабилност
                        confidence = ((hurst - 0.5f) * 2f).coerceIn(0f, 1f),
                        weight = PredictionModel.HURST_EXPONENT.baseWeight,
                    )

                // H < 0.45: Очаква се обръщане към средното (Negative Autocorrelation)
                hurst < 0.45f -> {
                    val reversalDir =
                        when (currentTrend) {
                            Direction.UP, Direction.STRONG_UP -> Direction.DOWN
                            Direction.DOWN, Direction.STRONG_DOWN -> Direction.UP
                            else -> Direction.SIDEWAYS
                        }

                    ModelVote(
                        model = PredictionModel.HURST_EXPONENT,
                        direction = reversalDir,
                        targetPrice = 0.0,
                        confidence = ((0.5f - hurst) * 2f).coerceIn(0f, 1f),
                        weight = PredictionModel.HURST_EXPONENT.baseWeight,
                    )
                }

                // 0.45 - 0.55: Random walk / Noise
                else ->
                    ModelVote(
                        model = PredictionModel.HURST_EXPONENT,
                        direction = Direction.SIDEWAYS,
                        targetPrice = 0.0,
                        confidence = 0f,
                        weight = PredictionModel.HURST_EXPONENT.baseWeight,
                    )
            }
    }
