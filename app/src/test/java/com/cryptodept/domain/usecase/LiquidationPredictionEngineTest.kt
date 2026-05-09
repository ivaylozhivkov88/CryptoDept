package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.LiquidationData
import com.cryptodept.domain.model.LiquidationLevel
import com.cryptodept.domain.model.LiquidationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidationPredictionEngineTest {
    private val engine = LiquidationPredictionEngine()

    @Test
    fun `predictMagneticZones returns closest significant levels`() {
        val currentPrice = 60000.0
        val data = LiquidationData(
            symbol = "BTC",
            longLiquidations24h = 0.0,
            shortLiquidations24h = 0.0,
            dominantSide = "LONGS",
            heatmapLevels = listOf(
                LiquidationLevel(61000.0, 0.0, 200_000_000.0, true), // Significant short squeeze
                LiquidationLevel(59000.0, 150_000_000.0, 0.0, true), // Significant long squeeze
                LiquidationLevel(70000.0, 0.0, 500_000_000.0, true), // Significant but far
                LiquidationLevel(60100.0, 0.0, 10_000.0, false), // Not significant
            ),
            timestamp = 0L
        )

        val result = engine.predictMagneticZones(currentPrice, data)

        assertEquals(3, result.size)
        assertEquals(59000.0, result[0].price, 0.1) // Closest is 59000 (1.6% dist)
        assertEquals(61000.0, result[1].price, 0.1) // Next is 61000 (1.6% dist)
        assertEquals(70000.0, result[2].price, 0.1) // Far one
    }

    @Test
    fun `predictMagneticZones correctly identifies squeeze type`() {
        val currentPrice = 50000.0
        val data = LiquidationData(
            symbol = "BTC",
            longLiquidations24h = 0.0,
            shortLiquidations24h = 0.0,
            dominantSide = "LONGS",
            heatmapLevels = listOf(
                LiquidationLevel(55000.0, 0.0, 200_000_000.0, true), // Above -> Short squeeze
                LiquidationLevel(45000.0, 200_000_000.0, 0.0, true), // Below -> Long squeeze
            ),
            timestamp = 0L
        )

        val result = engine.predictMagneticZones(currentPrice, data)

        assertEquals(LiquidationType.SHORT_SQUEEZE_POTENTIAL, result.find { it.price == 55000.0 }?.type)
        assertEquals(LiquidationType.LONG_SQUEEZE_POTENTIAL, result.find { it.price == 45000.0 }?.type)
    }

    @Test
    fun `predictMagneticZones handles empty or non-significant data`() {
        val result = engine.predictMagneticZones(60000.0, LiquidationData("BTC", 0.0, 0.0, "LONGS", emptyList(), 0L))
        assertTrue(result.isEmpty())
    }
}
