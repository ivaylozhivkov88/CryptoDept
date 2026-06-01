package com.cryptodept.data.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PredictionAccuracyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrediction(entity: PredictionAccuracyEntity)

    @Query("SELECT * FROM prediction_accuracy ORDER BY predictedAt DESC")
    fun getPagingSource(): PagingSource<Int, PredictionAccuracyEntity>

    @Query(
        """
        UPDATE prediction_accuracy 
        SET actualDirection = :actualDirection, 
            wasCorrect = :wasCorrect, 
            verifiedAt = :verifiedAt 
        WHERE id = :id
    """,
    )
    suspend fun updateVerification(
        id: Int,
        actualDirection: String,
        wasCorrect: Boolean,
        verifiedAt: Long,
    )

    @Query("SELECT * FROM prediction_accuracy WHERE model = :model")
    fun getAccuracyByModel(model: String): Flow<List<PredictionAccuracyEntity>>

    @Query("SELECT AVG(CASE WHEN wasCorrect = 1 THEN 1.0 ELSE 0.0 END) FROM prediction_accuracy WHERE wasCorrect IS NOT NULL")
    fun getOverallAccuracy(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM prediction_accuracy WHERE wasCorrect IS NOT NULL")
    fun getTotalVerifiedCount(): Flow<Int>

    @Query("SELECT * FROM prediction_accuracy WHERE verifiedAt IS NULL AND predictedAt < :olderThanMillis")
    suspend fun getUnverifiedOlderThan(olderThanMillis: Long): List<PredictionAccuracyEntity>

    @Query(
        """
        SELECT AVG(CASE WHEN wasCorrect = 1 THEN 1.0 ELSE 0.0 END) 
        FROM prediction_accuracy 
        WHERE model = :model AND wasCorrect IS NOT NULL
    """,
    )
    fun getModelAccuracy(model: String): Flow<Double?>

    @Query(
        """
        SELECT * FROM prediction_accuracy 
        WHERE coinId = :coinId 
        ORDER BY predictedAt DESC 
        LIMIT :limit
    """,
    )
    suspend fun getRecentForCoin(coinId: String, limit: Int): List<PredictionAccuracyEntity>

    @Query(
        """
        SELECT * FROM prediction_accuracy 
        WHERE coinId = :coinId AND model = :model 
        ORDER BY predictedAt DESC 
        LIMIT :limit
    """,
    )
    suspend fun getRecentForCoinAndModel(coinId: String, model: String, limit: Int): List<PredictionAccuracyEntity>
}
