package com.cryptodept.data.repository

import com.cryptodept.data.remote.source.FirebaseRemoteDataSource
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import com.google.common.truth.Truth.assertThat

class WhaleRepositoryTest {
    private val firebaseDataSource: FirebaseRemoteDataSource = mockk(relaxed = true)
    private lateinit var repository: WhaleRepositoryImpl

    @Before
    fun setup() {
        io.mockk.every { firebaseDataSource.getTerminalState() } returns flowOf(null)
        repository = WhaleRepositoryImpl(firebaseDataSource)
    }

    @Test
    fun `refreshWhaleTransactions executes without error`() = runTest {
        repository.refreshWhaleTransactions()
        assertThat(repository).isNotNull()
    }
}
