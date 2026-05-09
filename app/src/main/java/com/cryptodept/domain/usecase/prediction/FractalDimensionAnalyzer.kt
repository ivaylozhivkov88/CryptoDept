package com.cryptodept.domain.usecase.prediction

import javax.inject.Inject
import kotlin.math.*

/**
 * Математиката на самоподобието.
 * Fractal Dimension (D) измерва колко "сложен" и хаотичен е ценовият график.
 * - D близо до 1.0 -> Гладък тренд (висока предвидимост)
 * - D близо до 2.0 -> Хаотично движение (ниска предвидимост)
 */
class FractalDimensionAnalyzer
    @Inject
    constructor() {
        /**
         * Изчислява фракталната размерност по метода на Higuchi.
         * Най-стабилният алгоритъм за времеви редове в трейдинга.
         */
        fun calculate(
            prices: List<Double>,
            kMax: Int = 8,
        ): Float {
            val n = prices.size
            // Изискваме минимум 16 точки, за да имаме математически смислен резултат
            if (n < 16) return 1.5f

            val x = mutableListOf<Double>()
            val y = mutableListOf<Double>()

            for (k in 1..kMax) {
                val lengths = mutableListOf<Double>()

                for (m in 0 until k) {
                    var length = 0.0
                    val iterations = floor((n - m - 1).toDouble() / k).toInt()

                    if (iterations > 0) {
                        for (i in 1..iterations) {
                            length += abs(prices[m + i * k] - prices[m + (i - 1) * k])
                        }
                        // Нормализиращ фактор за Higuchi алгоритъма
                        val normFactor = (n - 1).toDouble() / (iterations * k)
                        lengths.add((length * normFactor) / k)
                    }
                }

                if (lengths.isNotEmpty()) {
                    // Използваме средната стойност на дължините за текущото k
                    y.add(ln(lengths.average()))
                    x.add(ln(1.0 / k))
                }
            }

            // Ако нямаме достатъчно точки за регресия, връщаме неутрална стойност
            if (x.size < 2) return 1.5f

            // Фракталната размерност е наклонът на log-log графиката
            val slope = calculateSlope(x, y)

            // Резултатът трябва да е между 1.0 (линия) и 2.0 (площ/пълен хаос)
            return slope.toFloat().coerceIn(1.0f, 2.0f)
        }

        /**
         * Превръща фракталната размерност в коефициент на предвидимост (0.0 до 1.0).
         * Този скор се използва за коригиране на доверието (confidence) на целия ансамбъл.
         */
        fun getPredictabilityScore(dimension: Float): Float {
            // Формула: Predictability = 2.0 - Dimension
            // Ако D=1.1 (силен тренд) -> Score=0.9 (висока предвидимост)
            // Ако D=1.9 (шум) -> Score=0.1 (ниска предвидимост)
            return (2.0f - dimension).coerceIn(0.0f, 1.0f)
        }

        /**
         * Линейна регресия за намиране на наклона (slope) на log-log точките.
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
            if (denominator == 0.0) return 0.0

            return (n * sumXY - sumX * sumY) / denominator
        }
    }
