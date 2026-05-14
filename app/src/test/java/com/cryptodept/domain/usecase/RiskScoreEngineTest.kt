package com.cryptodept.domain.usecase

import org.junit.Assert.*
import org.junit.Test

class RiskScoreEngineTest {
    private val riskEngine = RiskScoreEngine()

    @Test
    fun testVeryLowRiskScore() {
        // Favorable conditions: low RSI, low funding, bullish sentiment
        val score =
            riskEngine.calculate(
                rsi = 25.0,
                fundingRate = -0.01,
                longShortRatio = 0.8,
                fearGreedIndex = 20,
                exchangeInflowChange = -21.0, // Ensure < -20 for score 10
                openInterestChange = -5.0,
                priceChange24h = 5.0,
                macroRisk = 0.2,
            )
        assertTrue("Expected very low risk (overall 17)", score.overall < 20)
        assertEquals("Expected VERY_LOW level", RiskScoreEngine.RiskLevel.VERY_LOW, score.level)
    }

    @Test
    fun testHighRiskScore() {
        // Extreme conditions: high RSI, high funding, overbought
        val score =
            riskEngine.calculate(
                rsi = 90.0,
                fundingRate = 0.2, // Very high
                longShortRatio = 3.0,
                fearGreedIndex = 95,
                exchangeInflowChange = 50.0,
                openInterestChange = 30.0,
                priceChange24h = 20.0,
                macroRisk = 0.95,
            )
        assertTrue("Expected high risk (score: ${score.overall})", score.overall > 70)
        assertTrue(
            "Expected HIGH or EXTREME level",
            score.level == RiskScoreEngine.RiskLevel.HIGH || score.level == RiskScoreEngine.RiskLevel.EXTREME
        )
    }

    @Test
    fun testModerateRiskScore() {
        // Normal conditions
        val score =
            riskEngine.calculate(
                rsi = 75.0, // Elevated
                fundingRate = 0.05, // Elevated
                longShortRatio = 1.6, // Elevated
                fearGreedIndex = 65,
                exchangeInflowChange = 10.0,
                openInterestChange = 5.0,
                priceChange24h = 0.0,
                macroRisk = 0.6,
            )
        assertTrue("Expected moderate risk (score: ${score.overall})", score.overall in 40..60)
        assertEquals("Expected MODERATE level", RiskScoreEngine.RiskLevel.MODERATE, score.level)
    }

    @Test
    fun testComponentsPresent() {
        val score =
            riskEngine.calculate(
                rsi = 50.0,
                fundingRate = 0.02,
                longShortRatio = 1.2,
                fearGreedIndex = 50,
                exchangeInflowChange = 5.0,
                openInterestChange = 2.0,
                priceChange24h = 0.0,
            )
        assertTrue("Expected components present", score.components.isNotEmpty())
        assertTrue("Expected 4+ components", score.components.size >= 4)
        assertTrue("Expected dominant factors", score.dominantFactors.isNotEmpty())
    }
}
