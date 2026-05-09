package com.cryptodept.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.cryptodept.data.api.FearGreedApi
import com.cryptodept.domain.model.AlertDirection
import com.cryptodept.domain.repository.AlertsRepository
import com.cryptodept.domain.repository.CryptoRepository
import com.cryptodept.domain.repository.DerivativesRepository
import com.cryptodept.domain.usecase.*
import com.cryptodept.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class CryptoDataSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val cryptoRepository: CryptoRepository,
    private val taEngine: TechnicalAnalysisEngine,
    private val riskEngine: RiskScoreEngine,
    private val derivativesRepository: DerivativesRepository,
    private val fearGreedApi: FearGreedApi,
    private val confluenceDetector: ConfluenceAlertDetector,
    private val alertRepository: AlertsRepository,
    private val notificationHelper: NotificationHelper,
    private val evaluationEngine: AlertEvaluationEngine
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("CryptoDataSyncWorker", "Starting periodic background market analysis and alert check")
        
        return try {
            // 1. Refresh basic prices
            cryptoRepository.refreshPrices()
            
            // 2. Perform deep analysis for major coins (BTC)
            analyzeMarket()

            // 3. Check Alerts
            checkAllAlerts()
            
            Result.success()
        } catch (e: Exception) {
            Log.e("CryptoDataSyncWorker", "Error during sync: ${e.message}")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private suspend fun checkAllAlerts() {
        val alerts = alertRepository.getAlerts().first().filter { it.isActive }
        val compositeAlerts = alertRepository.getCompositeAlerts().first().filter { it.isActive }

        if (alerts.isEmpty() && compositeAlerts.isEmpty()) return

        // Process Simple Price Alerts
        alerts.forEach { alert ->
            val currentPrice = cryptoRepository.getCurrentPrice(alert.coinId)
            if (currentPrice <= 0) return@forEach

            val isTriggered = if (alert.direction == AlertDirection.ABOVE) {
                currentPrice >= alert.targetPrice
            } else {
                currentPrice <= alert.targetPrice
            }

            if (isTriggered) {
                notificationHelper.showPriceAlert(alert.coinSymbol, alert.targetPrice, currentPrice)
                alertRepository.updateAlert(alert.copy(isActive = false, isTriggered = true))
            }
        }

        // Process Composite Alerts
        compositeAlerts.forEach { alert ->
            val history = cryptoRepository.getOHLCData(alert.coinId, days = 14)
            if (history.isEmpty()) return@forEach

            val snapshot = taEngine.buildSnapshot(alert.coinSymbol, history)
            val result = evaluationEngine.evaluate(alert, snapshot)

            if (result.overallResult) {
                notificationHelper.showPriceAlert(
                    "${alert.coinSymbol} COMPOSITE",
                    0.0,
                    snapshot.price
                )
                alertRepository.updateCompositeAlert(alert.copy(isActive = false, isTriggered = true))
            }
        }
    }

    private suspend fun analyzeMarket() {
        val btcOHLC = cryptoRepository.getOHLCData("bitcoin", 30)
        val btcPrices = btcOHLC.map { it.close }
        val currentPrice = btcPrices.lastOrNull() ?: 0.0

        if (btcPrices.size >= 14) {
            val rsi = taEngine.calculateRSI(btcPrices)
            val macdResult = taEngine.calculateMACD(btcPrices)
            val fundingResult = derivativesRepository.getFundingRate("BTC")
            val funding = fundingResult.getOrNull()
            
            val fearGreedResponse = fearGreedApi.getFearGreedIndex()
            val fearGreed = fearGreedResponse.data.firstOrNull()?.value?.toIntOrNull() ?: 50
            val btcChange24h = cryptoRepository.getCachedChange24h("bitcoin")

            if (funding != null) {
                // Risk Engine calculation
                val riskScore = riskEngine.calculate(
                    rsi = rsi,
                    fundingRate = funding.binanceRate,
                    longShortRatio = 1.5,
                    fearGreedIndex = fearGreed,
                    exchangeInflowChange = 0.0,
                    openInterestChange = 0.0,
                    priceChange24h = btcChange24h
                )
                
                // Confluence Signal Detection
                val ema50 = taEngine.calculateEMA(btcPrices, 50).lastOrNull() ?: 0.0
                val ema200 = taEngine.calculateEMA(btcPrices, 200).lastOrNull() ?: 0.0

                confluenceDetector.detect(
                    coin = "BTC",
                    price = currentPrice,
                    rsi = rsi,
                    macdBullish = (macdResult.histogram.lastOrNull() ?: 0.0) > 0,
                    priceAboveEma50 = currentPrice > ema50,
                    priceAboveEma200 = currentPrice > ema200,
                    fundingRate = funding.binanceRate,
                    fearGreedIndex = fearGreed,
                    bollingerPosition = 0.5,
                    exchangeInflowChange = 0.0
                )

                Log.d("CryptoDataSyncWorker", "Market analyzed. Risk Score: ${riskScore.overall}")
            }
        }
    }

    companion object {
        private const val WORK_NAME = "crypto_data_sync_work"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<CryptoDataSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }
    }
}
