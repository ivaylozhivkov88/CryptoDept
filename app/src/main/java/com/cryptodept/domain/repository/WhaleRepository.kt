package com.cryptodept.domain.repository

import com.cryptodept.domain.model.WhaleTransaction
import kotlinx.coroutines.flow.Flow

interface WhaleRepository {
    fun getWhaleTransactions(): Flow<List<WhaleTransaction>>

    suspend fun refreshWhaleTransactions(): Result<Unit>
}
