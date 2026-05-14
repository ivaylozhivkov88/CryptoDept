package com.cryptodept.domain.usecase.prediction

import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.system.measureTimeMillis

class PredictionBenchmarkTest {

    @Before
    fun setup() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any<String>(), any<String>()) } returns 0
    }
    
    @Test
    fun `FFT predictor benchmark under 100ms`() = runBlocking {
        val prices = List(1000) { 100.0 + it }
        val predictor = FourierCyclePredictor()
        
        val time = measureTimeMillis {
            predictor.predict(prices, 30)
        }
        println("FFT Predictor took ${time}ms")
        assertTrue("FFT predictor too slow: ${time}ms", time < 100)
    }

    @Test
    fun `Monte Carlo benchmark under 500ms`() = runBlocking {
        val prices = List(1000) { 100.0 + it }
        val predictor = MonteCarloPredictor()
        
        val time = measureTimeMillis {
            predictor.simulate(prices, 30)
        }
        println("Monte Carlo Predictor took ${time}ms")
        assertTrue("Monte Carlo predictor too slow: ${time}ms", time < 500)
    }

    @Test
    fun `Wyckoff benchmark under 50ms`() = runBlocking {
        val prices = List(1000) { 100.0 + it }
        val volumes = List(1000) { 50.0 + it }
        val predictor = WyckoffPhaseDetector()
        
        val time = measureTimeMillis {
            predictor.predict(prices, volumes)
        }
        println("Wyckoff Predictor took ${time}ms")
        assertTrue("Wyckoff predictor too slow: ${time}ms", time < 50)
    }
}
