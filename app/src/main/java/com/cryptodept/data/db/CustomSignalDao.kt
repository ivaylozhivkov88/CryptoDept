package com.cryptodept.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomSignalDao {
    @Query("SELECT * FROM custom_signal_rules")
    fun getAllRules(): Flow<List<CustomSignalRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: CustomSignalRuleEntity)

    @Delete
    suspend fun deleteRule(rule: CustomSignalRuleEntity)

    @Query("UPDATE custom_signal_rules SET isActive = :isActive WHERE id = :id")
    suspend fun toggleRule(
        id: String,
        isActive: Boolean,
    )
}
