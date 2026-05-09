package com.cryptodept.viewmodel

import app.cash.turbine.test
import com.cryptodept.data.datastore.PreferencesService
import com.cryptodept.domain.usecase.DeepAnalysisResult
import com.cryptodept.domain.usecase.GenerateAnalysisReportUseCase
import com.cryptodept.domain.usecase.ObserveAnalysisHistoryUseCase
import com.cryptodept.domain.usecase.RunDeepAnalysisUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.any
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private val preferencesService: PreferencesService = mockk(relaxed = true)

    private lateinit var viewModel: AnalysisViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { preferencesService.isAdmin } returns flowOf(false)
        every { observeAnalysisHistory() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `analysisState emits Success when use case succeeds`() = runTest {
        val mockResult = mockk<DeepAnalysisResult>()
        coEvery { runDeepAnalysis.execute(any(), any()) } returns Result.success(mockResult)
        
        viewModel = AnalysisViewModel(runDeepAnalysis, generateReport, observeAnalysisHistory, preferencesService)
        
        viewModel.analysisState.test {
            // First item is Loading
            assertThat(awaitItem()).isInstanceOf(AnalysisUiState.Loading::class.java)
            // Second item is Success
            assertThat(awaitItem()).isInstanceOf(AnalysisUiState.Success::class.java)
        }
    }

    @Test
    fun `generateAIReport updates aiReport state`() = runTest {
        val mockResult = mockk<DeepAnalysisResult>()
        coEvery { generateReport.execute(any()) } returns flowOf("Report content")
        
        viewModel = AnalysisViewModel(runDeepAnalysis, generateReport, observeAnalysisHistory, preferencesService)
        
        viewModel.generateAIReport(mockResult)
        
        viewModel.aiReport.test {
            // Initial is null (or whatever is in state flow)
            // Skip the first null if needed or check it
            val item = awaitItem()
            if (item == null) {
                assertThat(awaitItem()).isEqualTo("CONNECTING...")
                assertThat(awaitItem()).isEqualTo("Report content")
            } else {
                 assertThat(item).isEqualTo("CONNECTING...")
                 assertThat(awaitItem()).isEqualTo("Report content")
            }
        }
    }
}
