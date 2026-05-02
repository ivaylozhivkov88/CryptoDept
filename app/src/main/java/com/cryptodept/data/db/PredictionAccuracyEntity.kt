package com.cryptodept.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prediction_accuracy")
data class PredictionAccuracyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val coinId: String,
    val model: String,          // "FOURIER", "ELLIOTT", "FRACTAL", "ENSEMBLE"
    val predictedDirection: String,  // "UP", "DOWN", "SIDEWAYS"
    val actualDirection: String?,    // null ако все още не е проверено
    val predictedAt: Long,
    val verifiedAt: Long?,
    val wasCorrect: Boolean?,
    val confidenceAtPrediction: Float
)