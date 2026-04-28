package com.cryptodept.domain.usecase.prediction

import com.cryptodept.domain.model.Direction
import com.cryptodept.domain.model.ModelVote
import com.cryptodept.domain.model.PredictionModel
import javax.inject.Inject
import kotlin.math.*

/**
 * Най-иновативният модел. Пазарите се движат в цикли.
 * Fourier Transform разлага ценовата серия на съставни цикли.
 * Намираме доминиращите цикли и предвиждаме следващия им пик/дъно.
 */
class FourierCyclePredictor @Inject constructor() {

    data class Cycle(
        val period: Double,    // Период в брой свещи
        val amplitude: Double, // Сила на цикъла
        val phase: Double,     // Текуща фаза
        val nextPeak: Int      // Свещи до следващия връх
    )

    /**
     * Предвижда бъдещото движение чрез анализ на доминиращите цикли.
     */
    fun predict(prices: List<Double>, periodsAhead: Int): ModelVote {
        // Изискваме минимум 32 свещи за базов честотен анализ
        if (prices.size < 32) return neutralVote()

        // 1. Премахваме линейния тренд, за да изолираме циклите (Detrending)
        val detrended = removeTrend(prices)

        // 2. Откриваме доминиращите цикли чрез Discrete Fourier Transform (DFT)
        val cycles = detectCycles(detrended)

        if (cycles.isEmpty()) return neutralVote()

        val currentPrice = prices.last()
        var predictedChange = 0.0

        // 3. Реконструираме сигнала за бъдещия период
        cycles.forEach { cycle ->
            // Изчисляваме бъдещата фаза след X периода
            val futurePhase = cycle.phase + (2.0 * PI * periodsAhead.toDouble() / cycle.period)
            // Добавяме влиянието на този цикъл към общата промяна
            predictedChange += cycle.amplitude * cos(futurePhase)
        }

        val predictedPrice = currentPrice + predictedChange

        // Confidence зависи от броя открити значими цикли
        val confidence = (cycles.size * 0.2f + 0.3f).coerceIn(0.4f, 0.85f)

        return ModelVote(
            model = PredictionModel.FOURIER_CYCLES,
            direction = when {
                predictedPrice > currentPrice * 1.015 -> Direction.UP
                predictedPrice < currentPrice * 0.985 -> Direction.DOWN
                else -> Direction.SIDEWAYS
            },
            targetPrice = predictedPrice,
            confidence = confidence,
            weight = PredictionModel.FOURIER_CYCLES.baseWeight
        )
    }

    private fun detectCycles(data: List<Double>): List<Cycle> {
        val n = data.size
        val cycles = mutableListOf<Cycle>()
        val stdDev = data.stdDev()

        // Търсим честоти (k) от 1 до n/2 (Nyquist frequency)
        for (k in 1 until n / 2) {
            var realPart = 0.0
            var imagPart = 0.0

            for (t in 0 until n) {
                val angle = 2.0 * PI * k * t / n
                realPart += data[t] * cos(angle)
                imagPart -= data[t] * sin(angle)
            }

            val amplitude = sqrt(realPart * realPart + imagPart * imagPart) / n
            val phase = atan2(imagPart, realPart)
            val period = n.toDouble() / k

            // Филтрираме само цикли със значима амплитуда спрямо шума
            if (amplitude > stdDev * 0.15) {
                val nextPeak = calculateNextPeak(phase, period)
                cycles.add(Cycle(period, amplitude, phase, nextPeak))
            }
        }

        // Вземаме топ 3 най-силни цикъла
        return cycles.sortedByDescending { it.amplitude }.take(3)
    }

    private fun removeTrend(prices: List<Double>): List<Double> {
        val n = prices.size
        if (n < 2) return prices
        // Линейна детрендизация чрез начална и крайна точка
        val slope = (prices.last() - prices.first()) / n.toDouble()
        return prices.mapIndexed { i, p -> p - (prices.first() + slope * i) }
    }

    private fun calculateNextPeak(phase: Double, period: Double): Int {
        val currentPos = (phase / (2.0 * PI)) * period
        var dist = (period - currentPos) % period
        if (dist < 0) dist += period
        return dist.toInt()
    }

    private fun List<Double>.stdDev(): Double {
        if (this.isEmpty()) return 0.0
        val mean = this.average()
        return sqrt(this.map { (it - mean).pow(2) }.average())
    }

    private fun neutralVote() = ModelVote(
        model = PredictionModel.FOURIER_CYCLES,
        direction = Direction.SIDEWAYS,
        targetPrice = 0.0,
        confidence = 0f,
        weight = PredictionModel.FOURIER_CYCLES.baseWeight
    )
}