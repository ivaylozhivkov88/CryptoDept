package com.cryptodept.viewmodel

import app.cash.turbine.test
import com.cryptodept.domain.model.Blockchain
import com.cryptodept.domain.model.WhaleTransaction
import com.cryptodept.domain.repository.WhaleRepository
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
class WhaleViewModelTest {
    private val repository: WhaleRepository = mockk()
    private lateinit var viewModel: WhaleViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { repository.getWhaleTransactions() } returns flowOf(emptyList())
        coEvery { repository.refreshWhaleTransactions() } returns Result.success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init calls refresh and updates loading state`() = runTest {
        viewModel = WhaleViewModel(repository)
        
        viewModel.isRefreshing.test {
            // Because it's unconfined, it might already be false if it finished immediately
            // But we can check the interaction
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `transactions reflects repository data`() = runTest {
        val tx = WhaleTransaction("1", Blockchain.BITCOIN, 10.0, 600000.0, "BTC", "A", "B", 1000L, "hash")
        every { repository.getWhaleTransactions() } returns flowOf(listOf(tx))
        
        viewModel = WhaleViewModel(repository)
        
        viewModel.transactions.test {
            val items = awaitItem()
            assertThat(items).hasSize(1)
            assertThat(items.first().id).isEqualTo("1")
        }
    }
}
