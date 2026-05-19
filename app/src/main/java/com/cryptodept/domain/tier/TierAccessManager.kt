package com.cryptodept.domain.tier

import com.cryptodept.data.billing.BillingService
import com.cryptodept.data.datastore.SubscriptionAccessManager
import com.cryptodept.util.TestModeFlag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for user's effective access tier.
 * 
 * Tier resolution priority (highest wins):
 *   1. ADMIN  — User logged in with hardcoded admin email
 *   2. PRO    — Active Google Play subscription
 *   3. FREE   — Default
 * 
 * Special case:
 *   - In DEBUG builds during TEST_PERIOD, returns PRO if BYPASS_PAYWALL_IN_DEBUG=true
 *   - Admin emails ALWAYS return ADMIN regardless of subscription state
 * 
 * Use this everywhere instead of:
 *   - Checking billingService.isPro directly
 *   - Checking preferencesService.isAdmin() directly
 *   - Hardcoded email comparison
 */
@Singleton
class TierAccessManager @Inject constructor(
    private val billingService: BillingService,
    private val subscription: SubscriptionAccessManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    /**
     * Current effective tier — reactive StateFlow.
     * 
     * Combines:
     *   - billingService.isPro flow (Google Play subscription)
     *   - subscription.getAdminStatusFlow() (email-based)
     *   - TestModeFlag.BYPASS_PAYWALL_IN_DEBUG (dev convenience)
     */
    val currentTier: StateFlow<AccessTier> = combine(
        billingService.isPro,
        subscription.getAdminStatusFlow(),
    ) { isPro, isAdmin ->
        resolveTier(isPro = isPro, isAdmin = isAdmin)
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = resolveTier(
            isPro = billingService.isPro.value, 
            isAdmin = subscription.isAdmin()
        ),
    )
    
    /**
     * Resolve tier from raw flags.
     */
    private fun resolveTier(isPro: Boolean, isAdmin: Boolean): AccessTier {
        // Admin always wins, regardless of subscription state
        if (isAdmin) return AccessTier.ADMIN
        
        // Debug bypass during test period (dev convenience)
        if (TestModeFlag.BYPASS_PAYWALL_IN_DEBUG) return AccessTier.PRO
        
        // Real Pro subscription
        if (isPro) return AccessTier.PRO
        
        // Default
        return AccessTier.FREE
    }
    
    /**
     * Synchronous tier check (use sparingly — prefer reactive).
     */
    fun getCurrentTier(): AccessTier = currentTier.value
    
    /**
     * Check if user has access to a specific feature.
     */
    fun hasAccess(feature: FeatureKey): Boolean {
        return currentTier.value.canAccess(feature.requiredTier)
    }
    
    /**
     * Reactive version of hasAccess — use in Compose with collectAsState.
     */
    fun hasAccessFlow(feature: FeatureKey): Flow<Boolean> {
        return currentTier.map { it.canAccess(feature.requiredTier) }
    }
    
    /**
     * Debug helper: dump current state.
     */
    fun dumpState(): String = buildString {
        appendLine("=== TierAccessManager State ===")
        appendLine("Current tier: ${currentTier.value}")
        appendLine("Is Pro (raw): ${billingService.isPro.value}")
        appendLine("Is Admin (raw): ${subscription.isAdmin()}")
        appendLine("Test period: ${TestModeFlag.IS_TEST_PERIOD}")
        appendLine("Bypass paywall (debug): ${TestModeFlag.BYPASS_PAYWALL_IN_DEBUG}")
        appendLine("===")
    }
}
