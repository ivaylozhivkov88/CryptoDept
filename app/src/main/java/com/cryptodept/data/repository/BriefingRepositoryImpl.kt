package com.cryptodept.data.repository

import com.cryptodept.data.db.IntelligenceBriefingDao
import com.cryptodept.data.db.IntelligenceBriefingEntity
import com.cryptodept.domain.repository.BriefingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BriefingRepositoryImpl @Inject constructor(
    private val briefingDao: IntelligenceBriefingDao
) : BriefingRepository {

    override fun getAllBriefings(): Flow<List<IntelligenceBriefingEntity>> {
        return briefingDao.getAllBriefings()
    }

    override suspend fun saveBriefing(briefing: IntelligenceBriefingEntity) {
        briefingDao.insertBriefing(briefing)
    }

    override suspend fun getLatestBriefing(): IntelligenceBriefingEntity? {
        return briefingDao.getLatestBriefing()
    }

    override suspend fun cleanup(daysToKeep: Int) {
        val threshold = System.currentTimeMillis() - (daysToKeep.toLong() * 24 * 60 * 60 * 1000)
        briefingDao.deleteOldBriefings(threshold)
    }
}
