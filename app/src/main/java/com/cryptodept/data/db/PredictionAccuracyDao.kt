package com.cryptodept.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PredictionAccuracyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrediction(entity: PredictionAccuracyEntity)

    @Query("""
        UPDATE prediction_accuracy 
        SET actualDirection = :actualDirection, 
            wasCorrect = :wasCorrect, 
            verifiedAt = :verifiedAt 
        WHERE id = :id
    """)
    suspend fun updateVerification(
        id: Int, 
        actualDirection: String, 
        wasCorrect: Boolean, 
        verifiedAt: Long
    )

    @Query("SELECT * FROM prediction_accuracy WHERE model = :model")
    fun getAccuracyByModel(model: String): Flow<List<PredictionAccuracyEntity>>

    @Query("SELECT AVG(CASE WHEN wasCorrect = 1 THEN 1.0 ELSE 0.0 END) FROM prediction_accuracy")
    fun getOverallAccuracy(): Flow<Double?>

    @Query("SELECT * FROM prediction_accuracy WHERE verifiedAt IS NULL AND predictedAt < :olderThanMillis")
    suspend fun getUnverifiedOlderThan(olderThanMillis: Long): List<PredictionAccuracyEntity>

    @Query("""
        SELECT AVG(CASE WHEN wasCorrect = 1 THEN 1.0 ELSE 0.0 END) 
        FROM prediction_accuracy 
        WHERE model = :model AND wasCorrect IS NOT NULL
    """)
    fun getModelAccuracy(model: String): Flow<Double?>
}