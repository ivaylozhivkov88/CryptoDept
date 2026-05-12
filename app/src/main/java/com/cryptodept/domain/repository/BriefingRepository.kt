package com.cryptodept.domain.repository

import com.cryptodept.data.db.IntelligenceBriefingEntity
import kotlinx.coroutines.flow.Flow

interface BriefingRepository {
    fun getAllBriefings(): Flow<List<IntelligenceBriefingEntity>>
    suspend fun saveBriefing(briefing: IntelligenceBriefingEntity)
    suspend fun getLatestBriefing(): IntelligenceBriefingEntity?
    suspend fun cleanup(daysToKeep: Int)
}
