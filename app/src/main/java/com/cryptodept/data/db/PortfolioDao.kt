package com.cryptodept.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioDao {
    @Query("SELECT * FROM portfolio")
    fun getAllEntries(): Flow<List<PortfolioEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entity: PortfolioEntity)

    @Delete
    suspend fun deleteEntry(entity: PortfolioEntity)

    @Query("DELETE FROM portfolio WHERE id = :id")
    suspend fun deleteEntryById(id: String)
}
