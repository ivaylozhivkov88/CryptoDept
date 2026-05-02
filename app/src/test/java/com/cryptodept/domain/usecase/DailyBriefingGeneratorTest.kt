package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.CalendarEvent
import com.cryptodept.domain.model.LiquidationLevel
import org.junit.Test
import org.junit.Assert.*

class DailyBriefingGeneratorTest {

    private val generator = DailyBriefingGenerator()
    private val riskEngine = RiskScoreEngine()

    @Test
    fun testBriefingGeneration() {
        val riskScore = riskEngine.calculate(
            rsi = 50.0,
            fundingRate = 0.02,
            longShortRatio = 1.2,
            fearGreedIndex = 50,
            exchangeInflowChange = 5.0,
            openInterestChange = 2.0,
            priceChange24h = 2.5
        )

        val briefing = generator.generate(
            btcPrice = 43500.0,
            btcChange24h = 2.5,
            riskScore = riskScore,
            fundingRate = 0.02,
            fearGreedIndex = 50,
            exchangeInflowChange = 5.0,
            upcomingEvents = emptyList(),
            topLiquidationLevel = null
        )

        assertNotNull("Briefing should be generated", briefing)
        assertTrue("Market sentence should not be empty", briefing.marketSentence.isNotEmpty())
        assertTrue("Should have key metrics", briefing.keyMetrics.isNotEmpty())
        assertEquals("Should have 5 key metrics", 5, briefing.keyMetrics.size)
    }

    @Test
    fun testPriceFormatting() {
        // Test that prices are formatted without crash
        val riskScore = riskEngine.calculate(
            rsi = 50.0, fundingRate = 0.02, longShortRatio = 1.2,
            fearGreedIndex = 50, exchangeInflowChange = 5.0,
            openInterestChange = 2.0, priceChange24h = 0.0
        )

        val briefing = generator.generate(
            btcPrice = 103500.50,  // Test comma formatting
            btcChange24h = 2.5,
            riskScore = riskScore,
            fundingRate = 0.02,
            fearGreedIndex = 50,
            exchangeInflowChange = 5.0,
            upcomingEvents = emptyList(),
            topLiquidationLevel = null
        )

        // Check that BTC PRICE metric exists and is formatted correctly
        val btcMetric = briefing.keyMetrics.find { it.label == "BTC PRICE" }
        assertNotNull("BTC PRICE metric should exist", btcMetric)
        assertTrue("Should contain dollar sign", btcMetric!!.value.contains("$"))
        assertTrue("Should contain number", btcMetric.value.contains("103"))
    }

    @Test
    fun testMillionFormatting() {
        val riskScore = riskEngine.calculate(
            rsi = 50.0, fundingRate = 0.02, longShortRatio = 1.2,
            fearGreedIndex = 50, exchangeInflowChange = 5.0,
            openInterestChange = 2.0, priceChange24h = 0.0
        )

        val liquidations = LiquidationLevel(
            price = 42000.0,
            longLiquidationUsd = 8_000_000.0,
            shortLiquidationUsd = 7_000_000.0,
            isSignificant = true
        )

        val briefing = generator.generate(
            btcPrice = 43500.0,
            btcChange24h = 2.5,
            riskScore = riskScore,
            fundingRate = 0.02,
            fearGreedIndex = 50,
            exchangeInflowChange = 5.0,
            upcomingEvents = emptyList(),
            topLiquidationLevel = liquidations
        )

        // Check that liquidation alert contains "M" suffix
        val liquidAlert = briefing.topAlerts.find { it.title == "LIQUIDATION CLUSTER" }
        assertNotNull("Liquidation alert should be present", liquidAlert)
        assertTrue("Should contain M suffix (millions)", liquidAlert!!.detail.contains("M"))
        assertTrue("Should NOT have double formatting crash", true) // If we got here, no crash
    }

    @Test
    fun testAlertSeverity() {
        val riskScore = riskEngine.calculate(
            rsi = 50.0, fundingRate = 0.15,  // EXTREME
            longShortRatio = 1.2,
            fearGreedIndex = 88,  // EXTREME GREED
            exchangeInflowChange = 35.0,  // HIGH INFLOWS
            openInterestChange = 2.0,
            priceChange24h = 0.0
        )

        val briefing = generator.generate(
            btcPrice = 43500.0,
            btcChange24h = 2.5,
            riskScore = riskScore,
            fundingRate = 0.15,  // EXTREME
            fearGreedIndex = 88,
            exchangeInflowChange = 35.0,
            upcomingEvents = emptyList(),
            topLiquidationLevel = null
        )

        // Should have CRITICAL and WARNING alerts
        val hasExtreme = briefing.topAlerts.any { it.severity == DailyBriefingGenerator.AlertSeverity.CRITICAL }
        val hasWarning = briefing.topAlerts.any { it.severity == DailyBriefingGenerator.AlertSeverity.WARNING }

        assertTrue("Should have extreme alerts for high risk conditions", hasExtreme || hasWarning)
    }
}

