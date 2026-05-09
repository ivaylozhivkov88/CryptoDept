package com.cryptodept.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkHealthDao {
    @Query("SELECT * FROM network_health WHERE id = 0")
    fun getNetworkHealth(): Flow<NetworkHealthEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNetworkHealth(health: NetworkHealthEntity)
}
