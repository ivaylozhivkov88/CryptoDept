package com.cryptodept.viewmodel

import app.cash.turbine.test
import com.cryptodept.domain.repository.AIProvider
import com.cryptodept.domain.repository.CryptoRepository
import com.cryptodept.domain.repository.JournalRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AICoachViewModelTest {
    private val aiProvider: AIProvider = mockk()
    private val journalRepository: JournalRepository = mockk()
    private val cryptoRepository: CryptoRepository = mockk()
    private val riskEngine: com.cryptodept.domain.usecase.RiskScoreEngine = mockk(relaxed = true)
    private lateinit var viewModel: AICoachViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial message is added on init`() {
        viewModel = AICoachViewModel(aiProvider, journalRepository, cryptoRepository, riskEngine)
        assertThat(viewModel.messages).hasSize(1)
        assertThat(viewModel.messages.first().sender).isEqualTo("COACH")
    }

    @Test
    fun `sendMessage adds user message and coach response`() = runTest {
        coEvery { aiProvider.sendMessage(any()) } returns flowOf("Response ", "from ", "AI")
        viewModel = AICoachViewModel(aiProvider, journalRepository, cryptoRepository, riskEngine)
        
        viewModel.sendMessage("Hello")
        
        assertThat(viewModel.messages).hasSize(3) // Init, User, Coach
        assertThat(viewModel.messages[1].text).isEqualTo("Hello")
        assertThat(viewModel.messages[2].text).isEqualTo("Response from AI")
    }
}
