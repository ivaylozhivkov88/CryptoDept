package com.cryptodept.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.cryptodept.MainActivity
import com.cryptodept.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CryptoMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        val title = message.notification?.title ?: message.data["title"] ?: "TERMINAL_SIGNAL"
        val body = message.notification?.body ?: message.data["body"] ?: "NEW_MARKET_DATA_RECEIVED"
        
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "cryptodept_alerts")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with app icon later
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

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        android.util.Log.d("FCM_TOKEN", ">>> NEW_TOKEN_RECEIVED: $token")
    }
}
