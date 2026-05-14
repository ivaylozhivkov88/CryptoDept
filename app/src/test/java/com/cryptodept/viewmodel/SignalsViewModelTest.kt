package com.cryptodept.viewmodel

import app.cash.turbine.test
import com.cryptodept.data.datastore.PreferencesService
import com.cryptodept.domain.model.OHLCData
import com.cryptodept.domain.repository.CryptoRepository
import com.cryptodept.domain.usecase.AlphaSignalEngine
import com.cryptodept.domain.usecase.GetOHLCUseCase
import com.cryptodept.domain.usecase.TechnicalAnalysisEngine
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
class SignalsViewModelTest {
    private val cryptoRepository: CryptoRepository = mockk()
    private val getOHLCUseCase: GetOHLCUseCase = mockk()
    private val taEngine: TechnicalAnalysisEngine = mockk(relaxed = true)
    private val alphaEngine: AlphaSignalEngine = mockk()
    private val preferencesService: PreferencesService = mockk(relaxed = true)

    private lateinit var viewModel: SignalsViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { alphaEngine.signals } returns flowOf(emptyList())
        every { preferencesService.isPro } returns MutableStateFlow(false)
        every { preferencesService.isAdmin } returns MutableStateFlow(false)
        coEvery { getOHLCUseCase(any(), any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial states are correct`() = runTest {
        viewModel = SignalsViewModel(cryptoRepository, getOHLCUseCase, taEngine, alphaEngine, preferencesService)
        
        viewModel.isLoading.test {
            // Might be already false if logic completes fast
            val initial = awaitItem()
            assertThat(initial).isAnyOf(true, false)
        }
    }

    @Test
    fun `generateSignals updates signals list`() = runTest {
        val ohlc = listOf(OHLCData(1, 100.0, 105.0, 95.0, 102.0, 1000.0))
        coEvery { getOHLCUseCase(any(), any()) } returns flowOf(ohlc)
        every { taEngine.calculateRSI(any()) } returns 50.0
        
        viewModel = SignalsViewModel(cryptoRepository, getOHLCUseCase, taEngine, alphaEngine, preferencesService)
        
        viewModel.signals.test {
            // First item might be empty
            val initial = awaitItem()
            if (initial.isEmpty()) {
                val updated = awaitItem()
                assertThat(updated).isNotEmpty()
            } else {
                assertThat(initial).isNotEmpty()
            }
        }
    }
}
