package com.cryptodept.domain.usecase.prediction

import android.util.Log
import com.cryptodept.BuildConfig
import com.cryptodept.domain.model.Direction
import com.cryptodept.domain.model.ModelVote
import com.cryptodept.domain.model.PredictionModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class FourierCyclePredictor @Inject constructor() {

    private data class Complex(val re: Double, val im: Double) {
        operator fun plus(other: Complex) = Complex(re + other.re, im + other.im)
        operator fun minus(other: Complex) = Complex(re - other.re, im - other.im)
        operator fun times(other: Complex) = Complex(
            re * other.re - im * other.im,
            re * other.im + im * other.re
        )
    }

    data class Cycle(
        val period: Double,
        val amplitude: Double,
        val phase: Double,
        val nextPeak: Int
    )

    private var lastResult: Pair<List<Double>, ModelVote>? = null

    suspend fun predict(prices: List<Double>, periodsAhead: Int): ModelVote = withContext(Dispatchers.Default) {
        if (lastResult?.first == prices) {
            return@withContext lastResult!!.second
        }

        if (prices.size < 32) return@withContext neutralVote()

        val startTime = System.currentTimeMillis()
        val detrended = removeTrend(prices)
        val cycles = detectCycles(detrended)

        if (cycles.isEmpty()) return@withContext neutralVote()

        val currentPrice = prices.last()
        var predictedChange = 0.0

        cycles.forEach { cycle ->
            val futurePhase = cycle.phase + (2.0 * PI * periodsAhead.toDouble() / cycle.period)
            predictedChange += cycle.amplitude * cos(futurePhase)
        }

        val predictedPrice = currentPrice + predictedChange
        val confidence = (cycles.size * 0.2f + 0.3f).coerceIn(0.4f, 0.85f)

        val result = ModelVote(
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

        val elapsed = System.currentTimeMillis() - startTime
        if (BuildConfig.DEBUG) {
            Log.d("FFT", "Completed in ${elapsed}ms for ${prices.size} points")
        }

        lastResult = prices to result
        result
    }

    private fun detectCycles(data: List<Double>): List<Cycle> {
        val n = data.size
        val m = nextPowerOf2(n)
        val paddedData = data.map { Complex(it, 0.0) }.toMutableList()
        while (paddedData.size < m) paddedData.add(Complex(0.0, 0.0))

        val spectrum = fft(paddedData)
        val cycles = mutableListOf<Cycle>()
        val stdDev = data.stdDev()

        for (k in 1 until m / 2) {
            val re = spectrum[k].re
            val im = spectrum[k].im
            val amplitude = sqrt(re * re + im * im) / n
            val phase = atan2(im, re)
            val period = m.toDouble() / k

            if (amplitude > stdDev * 0.15) {
                val nextPeak = calculateNextPeak(phase, period)
                cycles.add(Cycle(period, amplitude, phase, nextPeak))
            }
        }

        return cycles.sortedByDescending { it.amplitude }.take(3)
    }

    private fun fft(x: List<Complex>): List<Complex> {
        val n = x.size
        val y = x.toMutableList()

        // Bit-reversal permutation
        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                val temp = y[i]
                y[i] = y[j]
                y[j] = temp
            }
            var k = n / 2
            while (k <= j) {
                j -= k
                k /= 2
            }
            j += k
        }

        // Iterative butterfly operations
        var length = 2
        while (length <= n) {
            val angle = -2.0 * PI / length
            val wLen = Complex(cos(angle), sin(angle))
            for (i in 0 until n step length) {
                var w = Complex(1.0, 0.0)
                for (k in 0 until length / 2) {
                    val u = y[i + k]
                    val v = y[i + k + length / 2] * w
                    y[i + k] = u + v
                    y[i + k + length / 2] = u - v
                    w *= wLen
                }
            }
            length *= 2
        }
        return y
    }

    private fun nextPowerOf2(n: Int): Int {
        var m = 1
        while (m < n) m *= 2
        return m
    }

    private fun removeTrend(prices: List<Double>): List<Double> {
        val n = prices.size
        if (n < 2) return prices
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
