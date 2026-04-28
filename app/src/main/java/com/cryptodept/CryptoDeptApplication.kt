package com.cryptodept

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.cryptodept.data.remoteconfig.RemoteConfigManager
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp
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

            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager.createNotificationChannel(liveChannel)
            manager.createNotificationChannel(alertsChannel)
        }
    }
}
