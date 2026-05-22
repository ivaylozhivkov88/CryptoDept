package com.cryptodept.viewmodel

import app.cash.turbine.test
import com.cryptodept.data.datastore.PreferencesService
import com.cryptodept.domain.usecase.DeepAnalysisResult
import com.cryptodept.domain.usecase.GenerateAnalysisReportUseCase
import com.cryptodept.domain.usecase.ObserveAnalysisHistoryUseCase
import com.cryptodept.domain.usecase.RunDeepAnalysisUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalysisViewModelTest {
    private val runDeepAnalysis: RunDeepAnalysisUseCase = mockk()
    private val generateReport: GenerateAnalysisReportUseCase = mockk()
    private val observeAnalysisHistory: ObserveAnalysisHistoryUseCase = mockk()
    private val subscription: com.cryptodept.data.datastore.SubscriptionAccessManager = mockk(relaxed = true)
    private val remoteConfig: com.cryptodept.data.remoteconfig.RemoteConfigService = mockk(relaxed = true)
    private val demoMode: com.cryptodept.util.DemoModeProvider = mockk(relaxed = true)

    private lateinit var viewModel: AnalysisViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { subscription.isAdmin } returns MutableStateFlow(false)
        every { subscription.isPro } returns MutableStateFlow(true) // Pro users skip limits
        coEvery { subscription.getAiReportsCountToday() } returns 0
        every { remoteConfig.getFreeAiLimitDaily() } returns 10
        every { observeAnalysisHistory() } returns flowOf(emptyList())
        every { demoMode.demoActiveState } returns MutableStateFlow(false)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `analysisState emits Success when use case succeeds`() = runTest {
        val mockResult = mockk<DeepAnalysisResult>()
        coEvery { runDeepAnalysis.execute(any(), any()) } returns Result.success(mockResult)
        
        viewModel = AnalysisViewModel(runDeepAnalysis, generateReport, observeAnalysisHistory, subscription, remoteConfig, demoMode)
        
        viewModel.analysisState.test {
            // StateFlow might skip Loading and jump to Success in unconfined dispatcher
            val item = awaitItem()
            assertThat(item).isInstanceOf(AnalysisUiState::class.java)
            if (item is AnalysisUiState.Loading) {
                assertThat(awaitItem()).isInstanceOf(AnalysisUiState.Success::class.java)
            } else {
                assertThat(item).isInstanceOf(AnalysisUiState.Success::class.java)
            }
        }
    }

    @Test
    fun `generateAIReport updates aiReport state`() = runTest {
        val mockResult = mockk<DeepAnalysisResult>()
        coEvery { generateReport.execute(any()) } returns flowOf("Report content")
        
        viewModel = AnalysisViewModel(runDeepAnalysis, generateReport, observeAnalysisHistory, subscription, remoteConfig, demoMode)
        
        viewModel.aiReport.test {
            // Skip initial null
            assertThat(awaitItem()).isNull()
            
            viewModel.generateAIReport(mockResult)
            
            // In unconfined dispatcher, we might skip the empty string and get the full report
            val result = awaitItem()
            if (result == "") {
                assertThat(awaitItem()).isEqualTo("Report content")
            } else {
                assertThat(result).isEqualTo("Report content")
            }
        }
    }
}
