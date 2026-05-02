package com.cryptodept.service

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.cryptodept.MainActivity
import com.cryptodept.R
import com.cryptodept.data.api.BinanceWebSocketManager
import com.cryptodept.data.api.FearGreedApi
import com.cryptodept.domain.repository.AlertsRepository
import com.cryptodept.domain.repository.CryptoRepository
import com.cryptodept.domain.repository.DerivativesRepository // Важен импорт
import com.cryptodept.domain.usecase.ConfluenceAlertDetector
import com.cryptodept.domain.usecase.RiskScoreEngine
import com.cryptodept.domain.usecase.TechnicalAnalysisEngine
import com.cryptodept.util.NotificationChannels
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

    @Inject
    lateinit var derivativesRepository: DerivativesRepository

    @Inject
    lateinit var riskEngine: RiskScoreEngine

    @Inject
    lateinit var confluenceDetector: ConfluenceAlertDetector

    @Inject
    lateinit var taEngine: TechnicalAnalysisEngine

    @Inject
    lateinit var fearGreedApi: FearGreedApi

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var notificationManager: NotificationManager? = null
    private var refreshCount = 0

    companion object {
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
            NotificationChannels.LIVE_CHANNEL_ID,
            getString(R.string.notification_channel_live_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_live_description)
        }

        val alertChannel = NotificationChannel(
            NotificationChannels.ALERTS_CHANNEL_ID,
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

        return NotificationCompat.Builder(this, NotificationChannels.LIVE_CHANNEL_ID)
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
                    true
                }
                .collectLatest { ticker ->
                    val coinId = when (ticker.symbol.lowercase()) {
                        "btcusdt" -> "bitcoin"
                        "ethusdt" -> "ethereum"
                        "xrpusdt" -> "ripple"
                        else -> ticker.symbol.lowercase()
                    }

                    val price = ticker.lastPrice.toDoubleOrNull() ?: 0.0

                    updateNotification(ticker.symbol, price)
                    alertsRepository.checkAlerts(coinId, price)

                    if (ticker.symbol.lowercase() == "btcusdt") {
                        // Анализ на всеки ~300 тика (приблизително на всеки 5-10 минути според волатилността)
                        if (refreshCount % 300 == 0) {
                            analyzeMarket(price)
                        }
                        refreshCount++
                    }
                }
        }
    }

    private fun analyzeMarket(currentPrice: Double) {
        serviceScope.launch {
            try {
                val btcOHLC = cryptoRepository.getOHLCData("bitcoin", 30)
                val btcPrices = btcOHLC.map { it.close }

                if (btcPrices.size >= 14) {
                    val rsi = taEngine.calculateRSI(btcPrices)
                    val macdResult = taEngine.calculateMACD(btcPrices)
                    val fundingResult = derivativesRepository.getFundingRate("BTC")
                    val funding = fundingResult.getOrNull()

                    val fearGreedResponse = fearGreedApi.getFearGreedIndex()
                    val fearGreed = fearGreedResponse.data.firstOrNull()?.value?.toIntOrNull() ?: 50
                    val btcChange24h = cryptoRepository.getCachedChange24h("bitcoin")

                    if (funding != null) {
                        // 1. Изчисляване на Риск Скорост
                        val riskScore = riskEngine.calculate(
                            rsi = rsi,
                            fundingRate = funding.binanceRate,
                            longShortRatio = 1.5, // По подразбиране, ако нямаме API за това
                            fearGreedIndex = fearGreed,
                            exchangeInflowChange = 0.0,
                            openInterestChange = 0.0,
                            priceChange24h = btcChange24h
                        )

                        if (riskScore.overall > 75) {
                            showRiskAlert(riskScore)
                        }

                        // 2. Търсене на Конфлуенс (Сигнали)
                        val ema50 = taEngine.calculateEMA(btcPrices, 50).lastOrNull() ?: 0.0
                        val ema200 = taEngine.calculateEMA(btcPrices, 200).lastOrNull() ?: 0.0

                        val confluence = confluenceDetector.detect(
                            coin = "BTC",
                            price = currentPrice,
                            rsi = rsi,
                            macdBullish = (macdResult.histogram.lastOrNull() ?: 0.0) > 0,
                            priceAboveEma50 = currentPrice > ema50,
                            priceAboveEma200 = currentPrice > ema200,
                            fundingRate = funding.binanceRate,
                            fearGreedIndex = fearGreed,
                            bollingerPosition = 0.5, // Неутрално, ако липсва детайлно изчисление
                            exchangeInflowChange = 0.0
                        )

                        confluence?.let {
                            if (it.type.label.contains("STRONG") || it.type.label.contains("EXTREME")) {
                                showConfluenceAlert(it)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CryptoDept_Service", "Market Analysis Error: ${e.message}")
            }
        }
    }

    private fun updateNotification(symbol: String, price: Double) {
        val content = "$symbol: $${String.format("%.2f", price)}"
        notificationManager?.notify(NOTIFICATION_ID, createLiveNotification(content))
    }

    private fun showRiskAlert(riskScore: RiskScoreEngine.RiskScore) {
        val notification = NotificationCompat.Builder(this, NotificationChannels.ALERTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⚠️ HIGH MARKET RISK: ${riskScore.overall}/100")
            .setContentText(riskScore.recommendation)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager?.notify(2001, notification)
    }

    private fun showConfluenceAlert(confluence: ConfluenceAlertDetector.ConfluenceAlert) {
        val notification = NotificationCompat.Builder(this, NotificationChannels.ALERTS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🔥 ${confluence.type.label}")
            .setContentText("${confluence.direction} signal for ${confluence.coin}: ${confluence.suggestedAction}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager?.notify(2002, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}