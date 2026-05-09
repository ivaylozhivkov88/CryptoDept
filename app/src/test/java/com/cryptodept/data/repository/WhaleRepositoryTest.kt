package com.cryptodept.data.repository

import com.cryptodept.data.api.EtherscanWhaleClient
import com.cryptodept.data.api.HeliusWhaleClient
import com.cryptodept.data.api.MempoolWhaleClient
import com.cryptodept.domain.model.Blockchain
import com.cryptodept.domain.model.WhaleTransaction
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class WhaleRepositoryTest {
    private val etherscanClient: EtherscanWhaleClient = mockk()
    private val heliusClient: HeliusWhaleClient = mockk()
    private val btcClient: MempoolWhaleClient = mockk()

    private lateinit var repository: WhaleRepositoryImpl

    @Before
    fun setup() {
        repository = WhaleRepositoryImpl(etherscanClient, heliusClient, btcClient)
    }

    @Test
    fun `refreshWhaleTransactions aggregates and sorts transactions from all sources`() = runTest {
        val ethTx = WhaleTransaction("1", Blockchain.ETHEREUM, 100.0, 300000.0, "ETH", "A", "B", 1000L, "hash1")
        val solTx = WhaleTransaction("2", Blockchain.SOLANA, 1000.0, 150000.0, "SOL", "C", "D", 2000L, "hash2")
        val btcTx = WhaleTransaction("3", Blockchain.BITCOIN, 10.0, 600000.0, "BTC", "E", "F", 1500L, "hash3")

        coEvery { etherscanClient.fetchWhaleTransactions() } returns listOf(ethTx)
        coEvery { heliusClient.fetchWhaleTransactions() } returns listOf(solTx)
        coEvery { btcClient.fetchWhaleTransactions() } returns listOf(btcTx)

        repository.refreshWhaleTransactions()

        val results = repository.getWhaleTransactions().first()

        assertThat(results.size).isEqualTo(3)
        // Should be sorted by timestamp DESC: 2000, 1500, 1000
        assertThat(results[0].id).isEqualTo("2")
        assertThat(results[1].id).isEqualTo("3")
        assertThat(results[2].id).isEqualTo("1")
    }

    private fun assertThat(actual: Any?) = com.google.common.truth.Truth.assertThat(actual)
}
