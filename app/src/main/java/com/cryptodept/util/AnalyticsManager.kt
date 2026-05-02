package com.cryptodept.util

import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsManager @Inject constructor() {
    private val analytics = Firebase.analytics
    
    // Navigation events
    fun logScreenView(screenName: String) {
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
        }
    }
    
    // Trading tool usage
    fun logToolUsed(toolName: String) = analytics.logEvent("tool_used") { 
        param("tool_name", toolName) 
    }
    
    fun logPredictionViewed(coinId: String, verdict: String) = analytics.logEvent("prediction_viewed") {
        param("coin_id", coinId)
        param("verdict", verdict)
    }
    
    fun logAlertCreated(coinId: String, direction: String) = analytics.logEvent("alert_created") {
        param("coin_id", coinId)
        param("direction", direction)
    }
    
    fun logTradeLogged(direction: String, verdict: String) = analytics.logEvent("trade_logged") {
        param("direction", direction)
        param("verdict", verdict)
    }
    
    fun logCommandUsed(command: String) = analytics.logEvent("terminal_command") { 
        param("command", command) 
    }
    
    fun logProPaywallSeen() = analytics.logEvent("paywall_seen", null)
    
    fun logProPurchased(productId: String) = analytics.logEvent(FirebaseAnalytics.Event.PURCHASE) {
        param(FirebaseAnalytics.Param.ITEM_ID, productId)
    }
    
    // Errors
    fun logApiError(apiName: String, errorCode: Int) = analytics.logEvent("api_error") {
        param("api_name", apiName)
        param("error_code", errorCode.toLong())
    }
}
