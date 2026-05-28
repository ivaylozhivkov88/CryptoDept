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
        
        // --- FIREBASE APP CHECK CONFIGURATION ---
        if (BuildConfig.DEBUG) {
            try {
                val debugFactoryClass = Class.forName("com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory")
                val getInstanceMethod = debugFactoryClass.getMethod("getInstance")
                val factory = getInstanceMethod.invoke(null) as com.google.firebase.appcheck.AppCheckProviderFactory
                Firebase.appCheck.installAppCheckProviderFactory(factory)
            } catch (_: Exception) {
                // Fallback or log if debug factory is somehow missing even in debug
                Firebase.appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
            }
        } else {
            Firebase.appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
        }

        remoteConfigService.fetchAndActivate { }
        // socketLifecycleService.init() // Disabled (Q-001) - Real-time data now provided by Firebase centrally
        createNotificationChannels()
        DailyBriefingWorker.schedule(this)
        CryptoDataSyncWorker.schedule(this)
        com.cryptodept.service.NewsSyncWorker.schedule(this)
        com.cryptodept.service.AgentWatchdogWorker.schedule(this)
        com.cryptodept.service.AccuracyVerificationWorker.schedule(this)
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
