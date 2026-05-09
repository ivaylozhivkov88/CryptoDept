package com.cryptodept.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimulateLpUseCaseTest {
    private val simulateLp = SimulateLpUseCase()

    @Test
    fun `calculate IL correctly for 2x price divergence`() {
        val initialInvestment = 1000.0
        val priceChangeA = 1.0 // Stays same
        val priceChangeB = 2.0 // Doubles
        val apy = 0.0
        val days = 365

        val result = simulateLp(initialInvestment, priceChangeA, priceChangeB, apy, days)

        // IL for 2x divergence is ~5.72%
        assertEquals(-5.719, result.impermanentLoss, 0.01)
        assertTrue(result.finalValue < (initialInvestment * 0.5 * 1.0 + initialInvestment * 0.5 * 2.0))
    }

    @Test
    fun `yield compensates for IL`() {
        val initialInvestment = 1000.0
        val priceChangeA = 1.0
        val priceChangeB = 1.2 // 20% divergence
        val apy = 20.0 // 20% APY
        val days = 365

        val result = simulateLp(initialInvestment, priceChangeA, priceChangeB, apy, days)

        // IL for 1.2x is very small (~0.41%)
        // Net profit should be positive because APY > IL
        assertTrue(result.netProfit > 0)
    }

    @Test
    fun `no divergence means zero IL`() {
        val initialInvestment = 1000.0
        val result = simulateLp(initialInvestment, 1.5, 1.5, 0.0, 365)
        assertEquals(0.0, result.impermanentLoss, 0.001)
    }
}
