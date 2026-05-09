package com.cryptodept.domain.usecase

import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.cryptodept.data.db.PredictionAccuracyDao
import com.cryptodept.data.db.PredictionAccuracyEntity
import com.cryptodept.domain.repository.CryptoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PredictionAccuracyTracker
    @Inject
    constructor(
        private val dao: PredictionAccuracyDao,
        private val repository: CryptoRepository,
    ) {
        fun getHistoryPagingData(): Flow<PagingData<PredictionAccuracyEntity>> =
            Pager(
                config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                pagingSourceFactory = { dao.getPagingSource() },
            ).flow

        suspend fun recordPrediction(
            coinId: String,
            model: String,
            direction: String,
            confidence: Float,
            predictedAt: Long = System.currentTimeMillis(),
        ) = withContext(Dispatchers.IO) {
            val entity =
                PredictionAccuracyEntity(
                    coinId = coinId,
                    model = model,
                    predictedDirection = direction,
                    actualDirection = null,
                    predictedAt = predictedAt,
                    verifiedAt = null,
                    wasCorrect = null,
                    confidenceAtPrediction = confidence,
                )
            dao.insertPrediction(entity)
            Log.d("CryptoDept_AccuracyTracker", "Recorded prediction: $coinId $model $direction@${confidence * 100}%")
        }

        suspend fun verifyPredictions() =
            withContext(Dispatchers.IO) {
                try {
                    val unverified = dao.getUnverifiedOlderThan(System.currentTimeMillis() - 4 * 3600 * 1000)
                    Log.d("CryptoDept_AccuracyTracker", "Found ${unverified.size} unverified predictions to verify")

                    unverified.forEach { prediction ->
                        try {
                            val priceAtPrediction =
                                repository.getPriceAtTimestamp(
                                    prediction.coinId,
                                    prediction.predictedAt,
                                )
                            val currentPrice = repository.getCurrentPrice(prediction.coinId)

                            val priceChange = ((currentPrice - priceAtPrediction) / priceAtPrediction) * 100
                            val actualDir =
                                when {
                                    priceChange > 0.5 -> "UP"
                                    priceChange < -0.5 -> "DOWN"
                                    else -> "SIDEWAYS"
                                }
                            val wasCorrect = actualDir == prediction.predictedDirection

                            dao.updateVerification(
                                prediction.id,
                                actualDir,
                                wasCorrect,
                                System.currentTimeMillis(),
                            )

                            Log.d(
                                "CryptoDept_AccuracyTracker",
                                "Verified #${prediction.id}: predicted=${prediction.predictedDirection} " +
                                    "actual=$actualDir correct=$wasCorrect change=${String.format("%.2f", priceChange)}%",
                            )
                        } catch (e: Exception) {
                            Log.e("CryptoDept_AccuracyTracker", "Error verifying prediction #${prediction.id}", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CryptoDept_AccuracyTracker", "Error in verifyPredictions", e)
                }
            }

        fun getModelAccuracy(model: String): Flow<Float> =
            dao.getModelAccuracy(model).map { accuracy ->
                (accuracy ?: 0.0).coerceIn(0.0, 1.0).toFloat()
            }

        fun getOverallAccuracy(): Flow<Float> =
            dao.getOverallAccuracy().map { accuracy ->
                (accuracy ?: 0.0).coerceIn(0.0, 1.0).toFloat()
            }
    }
