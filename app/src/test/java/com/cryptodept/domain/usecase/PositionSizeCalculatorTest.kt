package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.PositionGrade
import org.junit.Test
import org.junit.Assert.*

class PositionSizeCalculatorTest {

    private val calculator = PositionSizeCalculator()

    @Test
    fun testBasicPositionSizing() {
        val result = calculator.calculate(
            portfolioSize = 10000.0,
            riskPercent = 2.0,
            entryPrice = 50000.0,
            stopLossPrice = 48000.0,
            takeProfitPrice = 55000.0,
            currentRiskScore = 50
        )

        assertEquals("Portfolio size should match", 10000.0, result.portfolioSize, 0.1)
        assertEquals("Max loss should be 2% of portfolio", 200.0, result.riskToLoseUsd, 0.1)
        assertTrue("Position size should be positive", result.positionSizeUsd > 0)
        assertTrue("R:R ratio should be > 1", result.riskRewardRatio > 1.0)
    }

    @Test
    fun testRiskAdjustmentAtHighRisk() {
        val result = calculator.calculate(
            portfolioSize = 10000.0,
            riskPercent = 2.0,
            entryPrice = 50000.0,
            stopLossPrice = 48000.0,
            takeProfitPrice = 55000.0,
            currentRiskScore = 80  // EXTREME RISK
        )

        // At score 80, position size should be reduced to 50%
        assertTrue("Position should be reduced 50% at risk 80", result.riskAdjustedSize <= result.positionSizeUsd * 0.51)
        assertTrue("Adjustment reason should mention EXTREME", result.riskAdjustmentReason.contains("EXTREME"))
    }

    @Test
    fun testGradeCalculation() {
        val poorRatio = calculator.calculate(
            portfolioSize = 10000.0,
            riskPercent = 2.0,
            entryPrice = 50000.0,
            stopLossPrice = 49500.0,  // Only 1% stop
            takeProfitPrice = 50250.0, // Only 0.5% profit target
            currentRiskScore = 50
        )
        assertEquals("Grade should be POOR for RR < 1.5", PositionGrade.POOR, poorRatio.grade)

        val excellentRatio = calculator.calculate(
            portfolioSize = 10000.0,
            riskPercent = 2.0,
            entryPrice = 50000.0,
            stopLossPrice = 48000.0,
            takeProfitPrice = 56000.0, // 4:2 = 2:1 ratio
            currentRiskScore = 50
        )
        assertEquals("Grade should be GOOD for RR 2+", true, excellentRatio.grade in listOf(
            PositionGrade.GOOD,
            PositionGrade.EXCELLENT
        ))
    }

    @Test
    fun testZeroDivisionSafety() {
        // Test when entry price is zero
        val result = calculator.calculate(
            portfolioSize = 10000.0,
            riskPercent = 2.0,
            entryPrice = 0.0,
            stopLossPrice = 0.0,
            takeProfitPrice = 100.0,
            currentRiskScore = 50
        )

        // Should not crash, should return safe values
        assertTrue("Position size should be calculable", result.positionSizeUsd >= 0)
    }
}

