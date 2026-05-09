package com.cryptodept.viewmodel

import app.cash.turbine.test
import com.cryptodept.data.datastore.PreferencesService
import com.cryptodept.domain.manager.DashboardLogService
import com.cryptodept.domain.model.CoinPrice
import com.cryptodept.domain.model.NetworkHealth
import com.cryptodept.domain.usecase.*
import com.cryptodept.util.AnalyticsService
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    private val observeTickerUseCase: ObserveTickerUseCase = mockk()
    private val getNetworkHealthUseCase: GetNetworkHealthUseCase = mockk()
    private val refreshPricesUseCase: RefreshPricesUseCase = mockk()
    private val getActionRecommendationUseCase: GetActionRecommendationUseCase = mockk()
    private val aiGenerator: AIReportGenerator = mockk()
    private val riskEngine: RiskScoreEngine = mockk()
    private val logService: DashboardLogService = mockk(relaxed = true)
    private val analytics: AnalyticsService = mockk(relaxed = true)
    private val preferencesService: PreferencesService = mockk(relaxed = true)

    private lateinit var viewModel: DashboardViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        val prices = listOf(CoinPrice(
            id = "bitcoin", symbol = "BTC", name = "Bitcoin", 
            currentPrice = 60000.0, priceChange24h = 1000.0, 
            priceChangePercentage24h = 1.5, marketCap = 1e12, 
            totalVolume = 3e10, high24h = 61000.0, low24h = 59000.0, 
            lastUpdated = System.currentTimeMillis()
        ))
        every { observeTickerUseCase() } returns flowOf(prices)
        every { preferencesService.isAdmin } returns flowOf(false)
        every { preferencesService.focusModeEnabled } returns flowOf(false)
        coEvery { getNetworkHealthUseCase() } returns Result.success(
            NetworkHealth("80 EH/s", "10 vB", "20 Gwei", 50, "Neutral")
        )
        coEvery { aiGenerator.generateShortSummary(any()) } returns Result.success("Bullish market")

        viewModel = DashboardViewModel(
            observeTickerUseCase,
            getNetworkHealthUseCase,
            refreshPricesUseCase,
            getActionRecommendationUseCase,
            aiGenerator,
            riskEngine,
            logService,
            analytics,
            preferencesService
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState emits Success when data is available`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state).isInstanceOf(DashboardUiState.Success::class.java)
            val success = state as DashboardUiState.Success
            assertThat(success.prices).isNotEmpty()
            assertThat(success.prices.first().symbol).isEqualTo("BTC")
        }
    }

    @Test
    fun `focusModeEnabled reflects preferences`() = runTest {
        every { preferencesService.focusModeEnabled } returns flowOf(true)
        // Need to re-init viewModel or mock was already used during init
        val vm = DashboardViewModel(
            observeTickerUseCase, getNetworkHealthUseCase, refreshPricesUseCase,
            getActionRecommendationUseCase, aiGenerator, riskEngine, logService,
            analytics, preferencesService
        )
        
        vm.focusModeEnabled.test {
            assertThat(awaitItem()).isTrue()
        }
    }
}
