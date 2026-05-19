package com.cryptodept.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.cryptodept.domain.tier.FeatureKey
import com.cryptodept.domain.tier.TierAccessManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Composable wrapper that conditionally shows content based on tier access.
 */
@Composable
fun AccessGate(
    feature: FeatureKey,
    modifier: Modifier = Modifier,
    hideIfLocked: Boolean = false,
    onUpgradeTap: (() -> Unit)? = null,
    lockedContent: @Composable (onUpgrade: () -> Unit) -> Unit = { onUpgrade ->
        UpgradeBanner(
            featureName = feature.displayName,
            description = feature.description,
            requiredTier = feature.requiredTier.displayName,
            onUpgradeClick = onUpgrade,
        )
    },
    content: @Composable () -> Unit,
) {
    val viewModel: AccessGateViewModel = hiltViewModel()
    val hasAccess by viewModel.hasAccessFlow(feature).collectAsState(initial = false)
    
    when {
        hasAccess -> {
            Box(modifier = modifier) {
                content()
            }
        }
        hideIfLocked -> {
            // Render nothing
        }
        else -> {
            Box(modifier = modifier) {
                lockedContent(onUpgradeTap ?: {})
            }
        }
    }
}

@HiltViewModel
class AccessGateViewModel @Inject constructor(
    private val tierAccessManager: TierAccessManager,
) : ViewModel() {
    fun hasAccessFlow(feature: FeatureKey) = tierAccessManager.hasAccessFlow(feature)
}
