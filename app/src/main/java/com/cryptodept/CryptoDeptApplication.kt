package com.cryptodept

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.cryptodept.data.remoteconfig.RemoteConfigManager
import com.cryptodept.service.DailyBriefingWorker
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class CryptoDeptApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var remoteConfigManager: RemoteConfigManager

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        Firebase.appCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )
        remoteConfigManager.fetchAndActivate { }
        createNotificationChannels()
        scheduleDailyBriefing()
    }

    private fun scheduleDailyBriefing() {
        val briefingRequest = PeriodicWorkRequestBuilder<DailyBriefingWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(calculateDelayUntil8AM(), TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_briefing",
            ExistingPeriodicWorkPolicy.KEEP,
            briefingRequest
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
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun createNotificationChannels() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val liveChannel = android.app.NotificationChannel(
                "cryptodept_live",
                "CryptoDept Live",
                android.app.NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Показва цени в реално време"
            }

            val alertsChannel = android.app.NotificationChannel(
                "cryptodept_alerts",
                "Price Alerts",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Нотификации при достигане на целева цена"
            }

            val briefingChannel = android.app.NotificationChannel(
                "cryptodept_briefing",
                "Daily Briefing",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Ежедневен пазарен бюлетин"
            }

            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager.createNotificationChannel(liveChannel)
            manager.createNotificationChannel(alertsChannel)
            manager.createNotificationChannel(briefingChannel)
        }
    }
}
