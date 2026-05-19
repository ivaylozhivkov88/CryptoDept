package com.cryptodept.domain.usecase.prediction

import com.cryptodept.data.db.PredictionAccuracyDao
import com.cryptodept.domain.model.Direction
import com.cryptodept.domain.model.EnsembleConsensus
import com.cryptodept.domain.prediction.ConfidenceMetrics
import com.cryptodept.domain.prediction.DataQuality
import com.cryptodept.domain.prediction.HistoricalAccuracy
import com.cryptodept.domain.prediction.ModelAgreement
import com.cryptodept.domain.prediction.VolatilityLevel
import javax.inject.Inject

class CalculateConfidenceMetricsUseCase @Inject constructor(
    private val accuracyDao: PredictionAccuracyDao,
) {
    suspend fun forEnsemble(
        coinId: String,
        consensus: EnsembleConsensus,
        currentPrice: Double,
        volatility24h: Double,
        lastDataUpdateMs: Long,
        dataPointCount: Int,
    ): ConfidenceMetrics {
        val agreeingCount = consensus.modelVotes.size - consensus.dissenterModels.size
        val agreement = ModelAgreement.fromRatio(
            agreeingModels = agreeingCount,
            totalModels = consensus.modelVotes.size,
        )
        
        val dataQuality = DataQuality.fromAge(lastDataUpdateMs, dataPointCount)
        val volatility = VolatilityLevel.fromVolatility(volatility24h)
        
        val invalidation = when (consensus.direction) {
            Direction.UP, Direction.STRONG_UP -> currentPrice * 0.95
            Direction.DOWN, Direction.STRONG_DOWN -> currentPrice * 1.05
            else -> null
        }
        
        val accuracy = calculateAccuracy(coinId, modelName = null)
        
        return ConfidenceMetrics(
            overallConfidence = consensus.overallConfidence,
            modelAgreement = agreement,
            dataQuality = dataQuality,
            volatilityWarning = volatility,
            invalidationLevel = invalidation,
            historicalAccuracy = accuracy,
        )
    }
    
    suspend fun forSingleModel(
        coinId: String,
        modelName: String,
    ): HistoricalAccuracy? {
        return calculateAccuracy(coinId, modelName)
    }
    
    private suspend fun calculateAccuracy(
        coinId: String,
        modelName: String?,
    ): HistoricalAccuracy? {
        val records = if (modelName != null) {
            accuracyDao.getRecentForCoinAndModel(coinId, modelName, limit = 30)
        } else {
            accuracyDao.getRecentForCoin(coinId, limit = 30)
        }
        
        val verifiedRecords = records.filter { it.wasCorrect != null }
        if (verifiedRecords.size < 5) return null
        
        val correct = verifiedRecords.count { it.wasCorrect == true }
        val accuracyPercent = (correct.toFloat() / verifiedRecords.size) * 100f
        
        return HistoricalAccuracy(
            sampleSize = verifiedRecords.size,
            accuracyPercent = accuracyPercent,
            timeframeText = "last ${verifiedRecords.size} predictions",
            modelName = modelName,
        )
    }
}
