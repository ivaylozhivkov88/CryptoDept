// STEP 8: Foreground Service for real-time monitoring
// Created: 2024-05-23
// Dependencies: Hilt, BinanceWebSocketManager, AlertsRepository, CryptoRepository
// Used by: Android System

package com.cryptodept.service

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.util.Log
import com.cryptodept.MainActivity
import com.cryptodept.R
import com.cryptodept.data.api.BinanceWebSocketManager
import com.cryptodept.domain.repository.AlertsRepository
import com.cryptodept.domain.repository.CryptoRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.retryWhen
import javax.inject.Inject
import kotlin.math.pow

@AndroidEntryPoint
class CryptoPriceForegroundService : Service() {

    @Inject
    lateinit var webSocketManager: BinanceWebSocketManager

    @Inject
    lateinit var cryptoRepository: CryptoRepository

    @Inject
    lateinit var alertsRepository: AlertsRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var notificationManager: NotificationManager? = null

    companion object {
        const val CHANNEL_LIVE_ID = "cryptodept_live"
        const val CHANNEL_ALERTS_ID = "cryptodept_alerts"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        webSocketManager.connect()
        observePrices()
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = createLiveNotification("Starting monitoring...")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID, 
                notification, 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannels() {
        val liveChannel = NotificationChannel(
            CHANNEL_LIVE_ID,
            getString(R.string.notification_channel_live_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_live_description)
        }

        val alertChannel = NotificationChannel(
            CHANNEL_ALERTS_ID,
            getString(R.string.notification_channel_alerts_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.notification_channel_alerts_description)
            enableVibration(true)
        }

        notificationManager?.createNotificationChannel(liveChannel)
        notificationManager?.createNotificationChannel(alertChannel)
    }

    private fun createLiveNotification(content: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_LIVE_ID)
            .setContentTitle(getString(R.string.notification_title_live))
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun observePrices() {
        serviceScope.launch {
            webSocketManager.observeTickerStream()
                .retryWhen { cause, attempt ->
                    Log.e("CryptoDept_FS", "WebSocket Stream failed (attempt $attempt): ${cause.message}")
                    val delayTime = minOf(1000L * (2.0.pow(attempt.toDouble())).toLong(), 30000L)
                    delay(delayTime)
                    true // Always retry
                }
                .collectLatest { ticker ->
                    val coinId = when (ticker.symbol.lowercase()) {
                        "btcusdt" -> "bitcoin"
                        "ethusdt" -> "ethereum"
                        "xrpusdt" -> "ripple"
                        else -> ticker.symbol.lowercase()
                    }
                    
                    val price = ticker.lastPrice.toDoubleOrNull() ?: 0.0
                    
                    // Update notification
                    updateNotification(ticker.symbol, price)
                    
                    // Check alerts
                    alertsRepository.checkAlerts(coinId, price)
                }
        }
    }

    private fun updateNotification(symbol: String, price: Double) {
        val content = "$symbol: $$price"
        notificationManager?.notify(NOTIFICATION_ID, createLiveNotification(content))
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
