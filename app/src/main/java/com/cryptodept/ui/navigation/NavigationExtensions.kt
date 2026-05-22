package com.cryptodept.ui.navigation

import androidx.navigation.NavController

import com.cryptodept.domain.tier.FeatureKey

/**
 * Navigate to paywall with optional reason context and feature context.
 */
fun NavController.navigateToPaywall(reason: String = "general", featureKey: FeatureKey? = null) {
    this.navigate(Screen.Paywall.createRoute(reason, featureKey?.name)) {
        launchSingleTop = true
    }
}
