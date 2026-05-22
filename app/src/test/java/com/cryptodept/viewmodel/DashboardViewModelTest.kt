package com.cryptodept.viewmodel

import app.cash.turbine.test
import com.cryptodept.data.datastore.SubscriptionAccessManager
import com.cryptodept.data.datastore.SystemSettingsManager
import com.cryptodept.data.remoteconfig.RemoteConfigService
import com.cryptodept.domain.manager.DashboardLogService
import com.cryptodept.domain.model.CoinPrice
import com.cryptodept.domain.model.NetworkHealth
import com.cryptodept.domain.usecase.*
import com.cryptodept.domain.tier.AccessTier
import com.cryptodept.domain.tier.TierAccessManager
import com.cryptodept.domain.usecase.prediction.GetDailyAIPickUseCase
import com.cryptodept.domain.usecase.whale.AggregateWhaleActivityUseCase
import com.cryptodept.util.AnalyticsService
import com.cryptodept.util.DemoModeProvider
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val aiGenerator: AIReportGenerator = mockk()
    private val riskEngine: RiskScoreEngine = mockk()
    private val logService: DashboardLogService = mockk(relaxed = true)
    private val analytics: AnalyticsService = mockk(relaxed = true)
    private val settings: SystemSettingsManager = mockk(relaxed = true)
    private val subscription: SubscriptionAccessManager = mockk(relaxed = true)
    private val agentCoordinator: MultiAgentCoordinator = mockk(relaxed = true)
    private val demoMode: DemoModeProvider = mockk(relaxed = true)
    private val remoteConfig: RemoteConfigService = mockk(relaxed = true)
    private val tierAccessManager: TierAccessManager = mockk(relaxed = true)
    private val firebaseDataSource: com.cryptodept.data.remote.source.FirebaseRemoteDataSource = mockk(relaxed = true)
    private val aggregateWhaleActivityUseCase: AggregateWhaleActivityUseCase = mockk(relaxed = true)
    private val getDailyAIPickUseCase: GetDailyAIPickUseCase = mockk(relaxed = true)
    private val getMacroIntelligenceUseCase: GetMacroIntelligenceUseCase = mockk(relaxed = true)

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
        every { subscription.isAdmin } returns MutableStateFlow(false)
        every { subscription.isPro } returns MutableStateFlow(false)
        every { tierAccessManager.currentTier } returns MutableStateFlow(AccessTier.FREE)
        every { settings.focusModeEnabled } returns flowOf(false)
        every { riskEngine.currentScore } returns MutableStateFlow(50)
        every { demoMode.demoActiveState } returns MutableStateFlow(false)
        every { firebaseDataSource.getTerminalState() } returns flowOf(null)
        every { logService.events } returns MutableStateFlow(emptyList())
        
        coEvery { getNetworkHealthUseCase() } returns Result.success(
            NetworkHealth("80 EH/s", "10 vB", "20 Gwei", 50, "Neutral")
        )
        coEvery { aiGenerator.generateShortSummary(any()) } returns Result.success("Bullish market")
        coEvery { getMacroIntelligenceUseCase() } returns Result.success(mockk(relaxed = true))

        viewModel = DashboardViewModel(
            observeTickerUseCase,
            getNetworkHealthUseCase,
            refreshPricesUseCase,
            aiGenerator,
            riskEngine,
            logService,
            analytics,
            settings,
            subscription,
            agentCoordinator,
            demoMode,
            remoteConfig,
            tierAccessManager,
            firebaseDataSource,
            aggregateWhaleActivityUseCase,
            getDailyAIPickUseCase,
            getMacroIntelligenceUseCase
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
}
