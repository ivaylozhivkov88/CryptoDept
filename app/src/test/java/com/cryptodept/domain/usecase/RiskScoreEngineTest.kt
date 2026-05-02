package com.cryptodept.domain.usecase

import org.junit.Test
import org.junit.Assert.*

class RiskScoreEngineTest {

    private val riskEngine = RiskScoreEngine()

    @Test
    fun testVeryLowRiskScore() {
        // Favorable conditions: low RSI, low funding, bullish sentiment
        val score = riskEngine.calculate(
            rsi = 25.0,
            fundingRate = -0.01,
            longShortRatio = 0.8,
            fearGreedIndex = 20,
            exchangeInflowChange = -20.0,
            openInterestChange = -5.0,
            priceChange24h = 5.0,
            macroRisk = 0.2
        )
        assertTrue("Expected very low risk", score.overall < 30)
        assertEquals("Expected VERY_LOW level", RiskScoreEngine.RiskLevel.VERY_LOW, score.level)
    }

    @Test
    fun testHighRiskScore() {
        // Extreme conditions: high RSI, high funding, overbought
        val score = riskEngine.calculate(
            rsi = 85.0,
            fundingRate = 0.12,
            longShortRatio = 2.0,
            fearGreedIndex = 90,
            exchangeInflowChange = 35.0,
            openInterestChange = 20.0,
            priceChange24h = 15.0,
            macroRisk = 0.9
        )
        assertTrue("Expected high risk", score.overall > 70)
        assertEquals("Expected HIGH or EXTREME level", true, score.level in listOf(
            RiskScoreEngine.RiskLevel.HIGH,
            RiskScoreEngine.RiskLevel.EXTREME
        ))
    }

    @Test
    fun testModerateRiskScore() {
        // Normal conditions
        val score = riskEngine.calculate(
            rsi = 50.0,
            fundingRate = 0.02,
            longShortRatio = 1.2,
            fearGreedIndex = 50,
            exchangeInflowChange = 5.0,
            openInterestChange = 2.0,
            priceChange24h = 0.0,
            macroRisk = 0.5
        )
        assertTrue("Expected moderate risk", score.overall in 30..70)
        assertTrue("Expected MODERATE level", score.level == RiskScoreEngine.RiskLevel.MODERATE)
    }

    @Test
    fun testComponentsPresent() {
        val score = riskEngine.calculate(
            rsi = 50.0,
            fundingRate = 0.02,
            longShortRatio = 1.2,
            fearGreedIndex = 50,
            exchangeInflowChange = 5.0,
            openInterestChange = 2.0,
            priceChange24h = 0.0
        )
        assertTrue("Expected components present", score.components.isNotEmpty())
        assertTrue("Expected 4+ components", score.components.size >= 4)
        assertTrue("Expected dominant factors", score.dominantFactors.isNotEmpty())
    }
}

