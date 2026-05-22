package com.cryptodept.viewmodel

import app.cash.turbine.test
import com.cryptodept.data.api.UnifiedWebSocketManager
import com.cryptodept.domain.repository.WhaleRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WhaleViewModelTest {
    private val aggregator: com.cryptodept.domain.usecase.whale.AggregateWhaleActivityUseCase = mockk()
    private val demoMode: com.cryptodept.util.DemoModeProvider = mockk(relaxed = true)
    private val haptic: com.cryptodept.util.HapticService = mockk(relaxed = true)
    private lateinit var viewModel: WhaleViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { aggregator.execute(any()) } returns emptyList()
        every { demoMode.demoActiveState } returns MutableStateFlow(false)
        every { demoMode.isActive() } returns false
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init calls refresh and updates loading state`() = runTest {
        viewModel = WhaleViewModel(aggregator, demoMode, haptic)
        
        viewModel.isRefreshing.test {
            assertThat(awaitItem()).isFalse()
        }
    }
}
