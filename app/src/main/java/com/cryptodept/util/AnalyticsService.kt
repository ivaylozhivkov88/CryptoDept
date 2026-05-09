package com.cryptodept.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

typealias AnalyticsManager = AnalyticsService

@Singleton
class AnalyticsService
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        private val crashlytics = FirebaseCrashlytics.getInstance()

        /**
         * Log a custom event to Firebase Analytics.
         * Respects user privacy (no PII).
         */
        fun logEvent(
            name: String,
            params: Bundle? = null,
        ) {
            firebaseAnalytics.logEvent(name, params)
        }

        /**
         * Log a screen view.
         */
        fun logScreenView(
            screenName: String,
            screenClass: String? = null,
        ) {
            val bundle =
                Bundle().apply {
                    putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                    putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass ?: screenName)
                }
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
        }

        /**
         * Log a non-fatal exception to Crashlytics.
         */
        fun recordException(
            throwable: Throwable,
            message: String? = null,
        ) {
            message?.let { crashlytics.log(it) }
            crashlytics.recordException(throwable)
        }

        /**
         * Add custom keys to Crashlytics for better debugging context.
         */
        fun setCustomKey(
            key: String,
            value: String,
        ) {
            crashlytics.setCustomKey(key, value)
        }

        fun setCustomKey(
            key: String,
            value: Boolean,
        ) {
            crashlytics.setCustomKey(key, value)
        }

        fun setCustomKey(
            key: String,
            value: Int,
        ) {
            crashlytics.setCustomKey(key, value)
        }

        /**
         * Standardized logging that also goes to Crashlytics logs.
         */
        fun log(message: String) {
            crashlytics.log(message)
            // Optionally also log to Logcat in debug
            if (com.cryptodept.BuildConfig.DEBUG) {
                android.util.Log.d("AnalyticsManager", message)
            }
        }

        /**
         * Set user ID for Crashlytics (Anonymized).
         */
        fun setUserId(userId: String) {
            crashlytics.setUserId(userId)
            firebaseAnalytics.setUserId(userId)
        }

        // Convenience methods for specific events
        fun logAlertCreated(
            coin: String,
            type: String,
        ) {
            logEvent(
                "alert_created",
                Bundle().apply {
                    putString("coin", coin)
                    putString("type", type)
                },
            )
        }

        fun logCommandUsed(command: String) {
            logEvent("command_used", Bundle().apply { putString("command", command) })
        }

        fun logProPaywallSeen() {
            logEvent("pro_paywall_seen")
        }

        fun logProPurchased(productId: String) {
            logEvent("pro_purchased", Bundle().apply { putString("product_id", productId) })
        }

        fun logTradeLogged(
            direction: String,
            status: String,
        ) {
            logEvent(
                "trade_logged",
                Bundle().apply {
                    putString("direction", direction)
                    putString("status", status)
                },
            )
        }
    }
