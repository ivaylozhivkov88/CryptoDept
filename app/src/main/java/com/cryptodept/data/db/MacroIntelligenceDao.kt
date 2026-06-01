package com.cryptodept.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "macro_intelligence")
data class MacroIntelligenceEntity(
    @PrimaryKey val id: Int = 0,
    val btcDominance: Double,
    val ethGasGwei: Int,
    val globalMarketCapUsd: Double,
    val altcoinSeasonIndex: Int,
    val timestamp: Long
)

@Dao
interface MacroIntelligenceDao {
    @Query("SELECT * FROM macro_intelligence WHERE id = 0")
    fun getMacroIntelligence(): Flow<MacroIntelligenceEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MacroIntelligenceEntity)
}
