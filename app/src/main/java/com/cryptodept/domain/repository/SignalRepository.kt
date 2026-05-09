package com.cryptodept.domain.repository

import com.cryptodept.domain.model.CustomSignalRule
import kotlinx.coroutines.flow.Flow

interface SignalRepository {
    fun getAllCustomRules(): Flow<List<CustomSignalRule>>

    suspend fun saveRule(rule: CustomSignalRule)

    suspend fun deleteRule(ruleId: String)

    suspend fun toggleRule(
        ruleId: String,
        isActive: Boolean,
    )
}
