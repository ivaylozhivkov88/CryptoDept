package com.cryptodept.service

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.cryptodept.R
import com.cryptodept.domain.repository.CryptoRepository
import com.cryptodept.domain.repository.DerivativesRepository
import com.cryptodept.domain.usecase.DailyBriefingGenerator
import com.cryptodept.domain.usecase.RiskScoreEngine
import com.cryptodept.util.NotificationChannels
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.*
import java.util.concurrent.TimeUnit

@HiltWorker
class DailyBriefingWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val cryptoRepository: CryptoRepository,
        private val derivativesRepository: DerivativesRepository,
        private val briefingGenerator: DailyBriefingGenerator,
        private val riskEngine: RiskScoreEngine,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            return try {
                val btcPrice = cryptoRepository.getCachedPrice("bitcoin")
                val btcChange = cryptoRepository.getCachedChange24h("bitcoin")
                val fundingResult = derivativesRepository.getFundingRate("BTC")
                val funding = fundingResult.getOrNull()
                val fearGreed = 50 // Default ако API fail

                if (funding == null || btcPrice == 0.0) return Result.retry()

                val riskScore =
                    riskEngine.calculate(
                        rsi = 50.0, // Simplified за briefing
                        fundingRate = funding.binanceRate,
                        longShortRatio = 1.5,
                        fearGreedIndex = fearGreed,
                        exchangeInflowChange = 0.0,
                        openInterestChange = 0.0,
                        priceChange24h = btcChange,
                        // whaleSellingDetected е премахнат
                    )

                val briefing =
                    briefingGenerator.generate(
                        btcPrice = btcPrice,
                        btcChange24h = btcChange,
                        riskScore = riskScore,
                        fundingRate = funding.binanceRate,
                        fearGreedIndex = fearGreed,
                        exchangeInflowChange = 0.0,
                        upcomingEvents = emptyList(),
                        topLiquidationLevel = null,
                        // topWhaleAlerts е премахнат[cite: 1]
                    )

                // Изпрати Daily Briefing push notification
                val notifManager =
                    applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                        as NotificationManager

                val notification =
                    NotificationCompat
                        .Builder(
                            applicationContext,
                            NotificationChannels.BRIEFING_CHANNEL_ID,
                        ).setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle("📊 DAILY MARKET BRIEFING")
                        .setContentText(briefing.marketSentence)
                        .setStyle(
                            NotificationCompat
                                .BigTextStyle()
                                .bigText(
                                    "${briefing.marketSentence}\n\nRISK: ${briefing.riskScore.level.label}\n${briefing.tradingSuggestion}",
                                ),
                        ).setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .build()

                notifManager.notify(9999, notification)

                Result.success()
            } catch (e: Exception) {
                Log.e("CryptoDept_Briefing", "Daily briefing failed: ${e.message}")
                Result.retry()
            }
        }

        companion object {
            private const val WORK_NAME = "daily_briefing"

            fun schedule(context: Context) {
                val briefingRequest =
                    PeriodicWorkRequestBuilder<DailyBriefingWorker>(24, TimeUnit.HOURS)
                        .setInitialDelay(calculateDelayUntil8AM(), TimeUnit.MILLISECONDS)
                        .setConstraints(
                            Constraints
                                .Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .setRequiresBatteryNotLow(true)
                                .build(),
                        ).build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    briefingRequest,
                )
            }

            private fun calculateDelayUntil8AM(): Long {
                val calendar = Calendar.getInstance()
                val now = calendar.timeInMillis
                calendar.set(Calendar.HOUR_OF_DAY, 8)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                if (calendar.timeInMillis <= now) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                return calendar.timeInMillis - now
            }
        }
    }
