package com.cryptodept.domain.tier

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface TierAccessManager {
    val currentTier: StateFlow<AccessTier>
    fun getCachedTier(): AccessTier
    fun hasAccess(feature: FeatureKey): Boolean
    fun hasAccessFlow(feature: FeatureKey): Flow<Boolean>
    fun dumpState(): String
}
