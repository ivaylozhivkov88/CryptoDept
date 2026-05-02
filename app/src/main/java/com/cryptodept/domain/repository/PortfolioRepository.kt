package com.cryptodept.domain.repository

import com.cryptodept.domain.model.PortfolioEntry
import com.cryptodept.domain.model.PortfolioSummary
import kotlinx.coroutines.flow.Flow

interface PortfolioRepository {
    fun getPortfolioEntries(): Flow<List<PortfolioEntry>>
    fun getPortfolioSummary(): Flow<PortfolioSummary>
    suspend fun addPosition(entry: PortfolioEntry)
    suspend fun removePosition(id: String)
}
