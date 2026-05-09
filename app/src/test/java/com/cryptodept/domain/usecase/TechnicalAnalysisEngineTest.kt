package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.OHLCData
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class TechnicalAnalysisEngineTest {
    private lateinit var engine: TechnicalAnalysisEngine

    @Before
    fun setup() {
        engine = TechnicalAnalysisEngine()
    }

    @Test
    fun `calculateRSI returns 100 for continuous gain`() {
        val prices = listOf(10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0, 18.0, 19.0, 20.0, 21.0, 22.0, 23.0, 24.0, 25.0)
        val rsi = engine.calculateRSI(prices)
        assertThat(rsi).isWithin(0.1).of(100.0)
    }

    @Test
    fun `calculateRSI returns 0 for continuous loss`() {
        val prices = listOf(25.0, 24.0, 23.0, 22.0, 21.0, 20.0, 19.0, 18.0, 17.0, 16.0, 15.0, 14.0, 13.0, 12.0, 11.0, 10.0)
        val rsi = engine.calculateRSI(prices)
        assertThat(rsi).isWithin(0.1).of(0.0)
    }

    @Test
    fun `calculateEMA returns correct values`() {
        val prices = listOf(10.0, 11.0, 12.0, 13.0, 14.0, 15.0)
        val ema = engine.calculateEMA(prices, 3)
        // Average of first 3: (10+11+12)/3 = 11.0
        // Next: (13 - 11) * (2/4) + 11 = 12.0
        // Next: (14 - 12) * 0.5 + 12 = 13.0
        // Next: (15 - 13) * 0.5 + 13 = 14.0
        assertThat(ema).containsExactly(11.0, 12.0, 13.0, 14.0).inOrder()
    }

    @Test
    fun `calculateMACD returns non-empty result for sufficient data`() {
        val prices = List(50) { it.toDouble() }
        val result = engine.calculateMACD(prices)
        assertThat(result.macdLine).isNotEmpty()
        assertThat(result.signalLine).isNotEmpty()
        assertThat(result.histogram).isNotEmpty()
    }

    @Test
    fun `detectPatterns identifies Hammer`() {
        val ohlc =
            listOf(
                OHLCData(0, 100.0, 100.0, 100.0, 100.0, 100.0), // Dummy
                OHLCData(1, 100.0, 101.0, 95.0, 99.0, 1000.0), // Hammer-ish: open=100, close=99, high=101, low=95
                // body = 1, lower wick = 4, upper wick = 1
                // last.lowerWick >= last.bodySize * 2 (4 >= 1*2) -> true
                // last.upperWick <= last.bodySize (1 <= 1) -> true
            )
        val patterns = engine.detectPatterns(ohlc)
        assertThat(patterns.any { it.pattern == TechnicalAnalysisEngine.CandlePattern.HAMMER }).isTrue()
    }
}
