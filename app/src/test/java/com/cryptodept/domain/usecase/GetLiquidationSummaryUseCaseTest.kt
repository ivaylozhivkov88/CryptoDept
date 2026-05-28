package com.cryptodept.domain.usecase

import com.cryptodept.data.api.LiquidationLevel
import com.cryptodept.data.api.LiquidationMapData
import com.cryptodept.data.api.LiquidationMapResponse
import com.cryptodept.domain.repository.CoinGlassRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class GetLiquidationSummaryUseCaseTest {

    private val repository: CoinGlassRepository = mockk()
    private lateinit var useCase: GetLiquidationSummaryUseCase

    @Before
    fun setup() {
        useCase = GetLiquidationSummaryUseCase(repository)
    }

    @Test
    fun `invoke should correctly calculate liquidation summary`() = runTest {
        // Arrange
        val symbol = "BTC"
        val currentPrice = 50000.0
        val liqLevels = listOf(
            LiquidationLevel(price = 49000.0, liqSize = 100.0, direction = "long"),
            LiquidationLevel(price = 48000.0, liqSize = 200.0, direction = "long"),
            LiquidationLevel(price = 51000.0, liqSize = 150.0, direction = "short"),
            LiquidationLevel(price = 52000.0, liqSize = 250.0, direction = "short")
        )
        val response = LiquidationMapResponse(
            code = "0",
            data = LiquidationMapData(chartData = null, liqList = liqLevels)
        )
        
        coEvery { repository.getLiquidationMap(symbol) } returns Result.success(response)

        // Act
        val result = useCase(symbol, currentPrice)

        // Assert
        assertThat(result.isSuccess).isTrue()
        val summary = result.getOrNull()!!
        
        assertThat(summary.symbol).isEqualTo(symbol)
        assertThat(summary.currentPrice).isEqualTo(currentPrice)
        
        assertThat(summary.nearestLongLevel).isEqualTo(49000.0)
        assertThat(summary.totalLongLiquidity).isEqualTo(300.0)
        assertThat(summary.nearestShortLevel).isEqualTo(51000.0)
        assertThat(summary.totalShortLiquidity).isEqualTo(400.0)
        assertThat(summary.longDominance).isWithin(0.01f).of(0.428f)
    }

    @Test
    fun `invoke should handle empty liquidation list`() = runTest {
        // Arrange
        val symbol = "ETH"
        val currentPrice = 3000.0
        val response = LiquidationMapResponse(code = "0", data = LiquidationMapData(chartData = null, liqList = emptyList()))
        coEvery { repository.getLiquidationMap(symbol) } returns Result.success(response)

        // Act
        val result = useCase(symbol, currentPrice)

        // Assert
        assertThat(result.isSuccess).isTrue()
        val summary = result.getOrNull()!!
        assertThat(summary.totalLongLiquidity).isEqualTo(0.0)
        assertThat(summary.totalShortLiquidity).isEqualTo(0.0)
        assertThat(summary.longDominance).isEqualTo(0.5f)
    }
}
