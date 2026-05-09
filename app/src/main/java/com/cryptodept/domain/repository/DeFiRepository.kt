package com.cryptodept.domain.repository

import com.cryptodept.domain.model.DeFiProtocol
import com.cryptodept.domain.model.DeFiYieldOpportunity

interface DeFiRepository {
    suspend fun getTopProtocols(): Result<List<DeFiProtocol>>
    suspend fun getTopYields(): Result<List<DeFiYieldOpportunity>>
}
