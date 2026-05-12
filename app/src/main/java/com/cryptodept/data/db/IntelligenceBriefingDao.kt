package com.cryptodept.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface IntelligenceBriefingDao {
    @Query("SELECT * FROM intelligence_briefings ORDER BY timestamp DESC")
    fun getAllBriefings(): Flow<List<IntelligenceBriefingEntity>>

    @Query("SELECT * FROM intelligence_briefings ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestBriefing(): IntelligenceBriefingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBriefing(briefing: IntelligenceBriefingEntity)

    @Query("DELETE FROM intelligence_briefings WHERE timestamp < :threshold")
    suspend fun deleteOldBriefings(threshold: Long)
}
