package com.cryptodept

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.cryptodept.data.remoteconfig.RemoteConfigService
import com.cryptodept.service.CryptoDataSyncWorker
import com.cryptodept.service.DailyBriefingWorker
import com.cryptodept.service.SocketLifecycleService
import com.cryptodept.util.NotificationChannels
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class CryptoDeptApplication :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var remoteConfigService: RemoteConfigService

    @Inject
    lateinit var socketLifecycleService: SocketLifecycleService

    override fun onCreate() {
        FirebaseApp.initializeApp(this)
        super.onCreate()
        setupCrashlytics()
        Firebase.appCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance(),
        )
        remoteConfigService.fetchAndActivate { }
        socketLifecycleService.init()
        createNotificationChannels()
        scheduleDailyBriefing()
        CryptoDataSyncWorker.schedule(this)
        com.cryptodept.service.NewsSyncWorker.schedule(this)
    }

    private fun setupCrashlytics() {
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            com.google.firebase.crashlytics.FirebaseCrashlytics
                .getInstance()
                .recordException(throwable)
            originalHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun scheduleDailyBriefing() {
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

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_briefing",
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

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .build()

    private fun createNotificationChannels() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val liveChannel =
                android.app
                    .NotificationChannel(
                        NotificationChannels.LIVE_CHANNEL_ID,
                        NotificationChannels.LIVE_CHANNEL_NAME,
                        android.app.NotificationManager.IMPORTANCE_LOW,
                    ).apply {
                        description = NotificationChannels.LIVE_CHANNEL_DESC
                    }

            val alertsChannel =
                android.app
                    .NotificationChannel(
                        NotificationChannels.ALERTS_CHANNEL_ID,
                        NotificationChannels.ALERTS_CHANNEL_NAME,
                        android.app.NotificationManager.IMPORTANCE_HIGH,
                    ).apply {
                        description = NotificationChannels.ALERTS_CHANNEL_DESC
                    }

            val briefingChannel =
                android.app
                    .NotificationChannel(
                        NotificationChannels.BRIEFING_CHANNEL_ID,
                        NotificationChannels.BRIEFING_CHANNEL_NAME,
                        android.app.NotificationManager.IMPORTANCE_DEFAULT,
                    ).apply {
                        description = NotificationChannels.BRIEFING_CHANNEL_DESC
                    }

            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager.createNotificationChannel(liveChannel)
            manager.createNotificationChannel(alertsChannel)
            manager.createNotificationChannel(briefingChannel)
        }
    }
}
