package com.cryptodept.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.domain.tier.FeatureKey
import com.cryptodept.viewmodel.SettingsViewModel

@Composable
fun ProGate(
    feature: FeatureKey? = null,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onLocked: @Composable (FeatureKey?) -> Unit = { /* Paywall handled in NavGraph or here */ },
    content: @Composable () -> Unit,
) {
    val tier by settingsViewModel.tierAccessManager.currentTier.collectAsState()
    val hasAccess = feature?.let { tier.canAccess(it.requiredTier) } ?: tier.isPaid

    if (hasAccess) {
        content()
    } else {
        onLocked(feature)
    }
}
