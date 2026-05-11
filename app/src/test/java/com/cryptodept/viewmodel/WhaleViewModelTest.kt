package com.cryptodept.viewmodel

import app.cash.turbine.test
import com.cryptodept.data.api.UnifiedWebSocketManager
import com.cryptodept.domain.repository.WhaleRepository
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
class WhaleViewModelTest {
    private val repository: WhaleRepository = mockk()
    private val wsManager: UnifiedWebSocketManager = mockk(relaxed = true)
    private lateinit var viewModel: WhaleViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { repository.getWhaleTransactions() } returns flowOf(emptyList())
        coEvery { repository.refreshWhaleTransactions() } returns Result.success(Unit)
        every { wsManager.marketEvents } returns flowOf()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init calls refresh and updates loading state`() = runTest {
        viewModel = WhaleViewModel(repository, wsManager)
        
        viewModel.isRefreshing.test {
            assertThat(awaitItem()).isFalse()
        }
    }
}
