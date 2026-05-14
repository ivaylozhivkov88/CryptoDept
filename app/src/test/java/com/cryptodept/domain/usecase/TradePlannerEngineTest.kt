package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class TradePlannerEngineTest {
    private val taEngine = mock(TechnicalAnalysisEngine::class.java)
    private val riskEngine = RiskScoreEngine()
    private val mtfAnalyzer = mock(MultiTimeframeAnalyzer::class.java)
    private val hapticManager = mock(com.cryptodept.util.HapticManager::class.java)

    private val tradePlanner = TradePlannerEngine(taEngine, riskEngine, mtfAnalyzer, hapticManager)

    @Before
    fun setup() {
        val mtfReport =
            MTFConsensus(
                timeframes = listOf(
                    TimeframeSignal("4H", TrendDirection.UP, 50.0, MTFMacdSignal.BULLISH, EmaSignal.ABOVE_ALL, OverallSignal.BUY, 42),
                    TimeframeSignal("1D", TrendDirection.UP, 50.0, MTFMacdSignal.BULLISH, EmaSignal.ABOVE_ALL, OverallSignal.BUY, 30),
                    TimeframeSignal("1W", TrendDirection.UP, 50.0, MTFMacdSignal.BULLISH, EmaSignal.ABOVE_ALL, OverallSignal.BUY, 13)
                ),
                bullishCount = 3,
                bearishCount = 0,
                neutralCount = 0,
                consensus = OverallSignal.BUY,
                interpretation = "Strong buy",
                tradingBias = "LONG"
            )
        runBlocking {
            `when`(mtfAnalyzer.analyze("BTC")).thenReturn(mtfReport)
        }
    }

    @Test
    fun testStrongBullishSetup() =
        runBlocking {
            val setup =
                tradePlanner.evaluate(
                    coin = "BTC",
                    direction = TradeDirectionType.LONG,
                    entryPrice = 50000.0,
                    stopLoss = 48000.0,
                    takeProfit = 55000.0,
                    currentRsi = 40.0, // Bullish (not overbought)
                    fundingRate = 0.02, // Normal
                    fearGreedIndex = 35, // Contrarian bullish
                    riskScore = 40, // Moderate risk
                )

            // Low risk score + good R:R + RSI support = should be setup
            assertTrue("Expected setup verdict (not AVOID)", setup.verdict != SetupVerdict.AVOID)
        }

    @Test
    fun testAvoidHighRisk() =
        runBlocking {
            val setup =
                tradePlanner.evaluate(
                    coin = "BTC",
                    direction = TradeDirectionType.LONG,
                    entryPrice = 50000.0,
                    stopLoss = 48000.0,
                    takeProfit = 51000.0, // Bad R:R (0.5)
                    currentRsi = 75.0, // Overbought
                    fundingRate = 0.15, // Extreme (CRITICAL risk factor)
                    fearGreedIndex = 90, // Extreme greed
                    riskScore = 85, // EXTREME RISK - CRITICAL
                )

            // CRITICAL fails should trigger AVOID
            assertTrue("Expected AVOID verdict", setup.verdict == SetupVerdict.AVOID)
        }

    @Test
    fun testChecklistBuilding() =
        runBlocking {
            val setup =
                tradePlanner.evaluate(
                    coin = "BTC",
                    direction = TradeDirectionType.LONG,
                    entryPrice = 50000.0,
                    stopLoss = 48000.0,
                    takeProfit = 55000.0,
                    currentRsi = 45.0,
                    fundingRate = 0.01,
                    fearGreedIndex = 50,
                    riskScore = 50,
                )

            assertTrue("Checklist should have items", setup.checklist.isNotEmpty())
            assertTrue("Should have at least 8 checklist items", setup.checklist.size >= 8)
            assertTrue("Should have multiple categories", setup.checklist.distinctBy { it.category }.size > 1)
        }
}
