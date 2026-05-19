package com.cryptodept.ui.navigation

import androidx.navigation.NavController

/**
 * Navigate to paywall with optional reason context.
 * 
 * Reason is shown in paywall pitch to personalize the upgrade ask.
 * Example: "whale_tracker" → "Unlock Live Whale Activity"
 */
fun NavController.navigateToPaywall(reason: String = "general") {
    this.navigate(Screen.Paywall.createRoute(reason)) {
        launchSingleTop = true
    }
}
