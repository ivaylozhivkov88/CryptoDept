package com.cryptodept.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.cryptodept.MainActivity
import com.cryptodept.util.NotificationChannels
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CryptoMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        android.util.Log.d("FCM_TOKEN", ">>> YOUR_TOKEN: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["title"] ?: "TERMINAL_SIGNAL"
        val body = message.notification?.body ?: message.data["body"] ?: "NEW_MARKET_DATA_RECEIVED"
        val deepLink = message.data["link"] // Check if a custom deep link was sent

        showNotification(title, body, deepLink)
    }

    private fun showNotification(
        title: String,
        body: String,
        deepLink: String?
    ) {
        val intent = if (!deepLink.isNullOrBlank()) {
            Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)).apply {
                setClass(this@CryptoMessagingService, MainActivity::class.java)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        } else {
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NotificationChannels.ALERTS_CHANNEL_ID)
            .setSmallIcon(com.cryptodept.R.drawable.ic_notification) // Using our custom icon
            .setColor(0xFF00FF41.toInt()) // WallStreet Green
            .setContentTitle(">>> $title")
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
