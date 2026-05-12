package com.cryptodept.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.cryptodept.domain.model.AgentStatus
import com.cryptodept.domain.model.MarketDataSnapshot
import com.cryptodept.domain.repository.BriefingRepository
import com.cryptodept.data.db.IntelligenceBriefingEntity
import com.cryptodept.domain.usecase.GetNetworkHealthUseCase
import com.cryptodept.domain.usecase.MultiAgentCoordinator
import com.cryptodept.domain.usecase.RiskScoreEngine
import com.cryptodept.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class AgentWatchdogWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val agentCoordinator: MultiAgentCoordinator,
    private val getNetworkHealth: GetNetworkHealthUseCase,
    private val riskEngine: RiskScoreEngine,
    private val briefingRepository: BriefingRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("AgentWatchdog", "INITIATING_AUTONOMOUS_SILENT_SCAN...")

        return try {
            val healthResult = getNetworkHealth()
            val snapshot = if (healthResult.isSuccess) {
                val health = healthResult.getOrThrow()
                MarketDataSnapshot(
                    price = 0.0, // Ticker not available in this scope easily, using health context
                    rsi = 50.0,
                    macdSignal = "N/A",
                    ema50Signal = "N/A",
                    ema200Signal = "N/A",
                    bollingerPosition = "N/A",
                    fundingRate = 0.0,
                    fundingLevel = "N/A",
                    longLiquidations24h = 0.0,
                    shortLiquidations24h = 0.0,
                    fearGreedIndex = health.fearGreedIndex,
                    newsSentiment = health.socialPulseLabel.uppercase(),
                    wyckoffPhase = "N/A",
                    elliottWave = "N/A",
                    riskScore = riskEngine.currentScore.value,
                    priceChange24h = 0.0,
                    btcDominance = 50.0,
                    sp500Change = 0.0,
                    dxyChange = 0.0,
                )
            } else {
                return Result.retry()
            }

            val report = agentCoordinator.runOrchestration(snapshot)

            if (report.anomalyScore > 80) {
                Log.w("AgentWatchdog", "CRITICAL_ANOMALY_DETECTED: ${report.anomalyScore}")
                
                // Save to repository
                briefingRepository.saveBriefing(
                    IntelligenceBriefingEntity(
                        timestamp = System.currentTimeMillis(),
                        summary = report.summary,
                        anomalyScore = report.anomalyScore,
                        sentiment = snapshot.newsSentiment,
                        riskScore = snapshot.riskScore,
                        evidence = report.details.entries.joinToString { "${it.key}=${it.value}" }
                    )
                )

                notificationHelper.showHighPriorityNotification(
                    title = ">>> AGENTIC_OVERSIGHT_ALERT",
                    message = report.summary.take(120) + "..."
                )
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("AgentWatchdog", "FATAL_WATCHDOG_ERROR: ${e.message}")
            Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "AgentWatchdogWork"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<AgentWatchdogWorker>(1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
