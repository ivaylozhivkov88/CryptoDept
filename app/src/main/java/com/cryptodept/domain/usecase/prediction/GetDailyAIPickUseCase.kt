package com.cryptodept.domain.usecase.prediction

import com.cryptodept.data.datastore.UserSessionManager
import com.cryptodept.domain.repository.CryptoRepository
import com.cryptodept.domain.usecase.GetOHLCUseCase
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject

class GetDailyAIPickUseCase @Inject constructor(
    private val predictionEngine: PredictionEnsembleEngine,
    private val cryptoRepository: CryptoRepository,
    private val ohlcUseCase: GetOHLCUseCase,
    private val session: UserSessionManager,
) {
    
    /**
     * Get today's AI pick. Cached for 24h.
     */
    suspend fun execute(): DailyAIPick? {
        val todayKey = todayKey()
        
        // Try cache first
        loadFromCache(todayKey)?.let { return it }
        
        // Determine which coin to predict
        val topCoins = cryptoRepository.getAllCoinPrices().first()
            .sortedByDescending { it.marketCap }
            .take(10)
            
        if (topCoins.isEmpty()) return null
        
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val selectedCoin = topCoins[dayOfYear % topCoins.size]
        
        // Fetch OHLC data for prediction
        val ohlc = ohlcUseCase(selectedCoin.id, 30).first()
        if (ohlc.isEmpty()) return null
        
        val closes = ohlc.map { it.close }
        val volumes = ohlc.map { it.volume }
        
        // Run ensemble prediction
        val prediction = try {
            predictionEngine.generatePrediction(selectedCoin.id, closes, volumes)
        } catch (e: Exception) {
            null
        } ?: return null
        
        val pick = DailyAIPick(
            date = todayKey,
            coinId = selectedCoin.id,
            coinSymbol = selectedCoin.symbol,
            coinName = selectedCoin.name,
            direction = prediction.ensembleConsensus.direction.name,
            confidence = prediction.ensembleConsensus.overallConfidence,
            targetPrice = prediction.prediction24h.mid,
            historicalAccuracy = prediction.modelsAgreement,
            sampleSize = 100, // Placeholder
            generatedAt = System.currentTimeMillis(),
        )
        
        saveToCache(todayKey, pick)
        return pick
    }
    
    private fun todayKey(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
    }
    
    private suspend fun loadFromCache(key: String): DailyAIPick? {
        val json = session.getString("daily_ai_pick_$key", null) ?: return null
        return try {
            Gson().fromJson(json, DailyAIPick::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun saveToCache(key: String, pick: DailyAIPick) {
        session.putString("daily_ai_pick_$key", Gson().toJson(pick))
    }
}

data class DailyAIPick(
    val date: String,
    val coinId: String,
    val coinSymbol: String,
    val coinName: String,
    val direction: String,
    val confidence: Float,
    val targetPrice: Double?,
    val historicalAccuracy: Float?,
    val sampleSize: Int?,
    val generatedAt: Long,
)
