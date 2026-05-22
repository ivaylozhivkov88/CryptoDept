package com.cryptodept.domain.tier

import com.cryptodept.data.billing.BillingService
import com.cryptodept.data.datastore.SubscriptionAccessManager
import com.cryptodept.util.TestModeFlag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi

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
    private val firebaseDataSource: com.cryptodept.data.remote.source.FirebaseRemoteDataSource,
    private val authService: com.cryptodept.data.auth.AuthService,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Listen to server-side authority if user is logged in.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val serverTierFlow: Flow<AccessTier?> = authService.currentUser.flatMapLatest { user ->
        if (user == null) kotlinx.coroutines.flow.flowOf(null)
        else firebaseDataSource.getUserTier(user.uid).map { tierStr ->
            when (tierStr) {
                "PRO" -> AccessTier.PRO
                "ADMIN" -> AccessTier.ADMIN
                else -> null
            }
        }
    }
    
    /**
     * Current effective tier — reactive StateFlow.
     * 
     * Priority: Hardcoded Email > ADMIN > Server PRO > Local PRO > FREE
     */
    val currentTier: StateFlow<AccessTier> = combine(
        billingService.isPro,
        subscription.getAdminStatusFlow(),
        serverTierFlow,
        authService.currentUser
    ) { localPro, isAdmin, serverTier, user ->
        resolveTier(localPro = localPro, isAdmin = isAdmin, serverTier = serverTier, user = user)
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = resolveTier(
            localPro = billingService.isPro.value, 
            isAdmin = subscription.isAdmin(),
            serverTier = null,
            user = authService.currentUser.value
        ),
    )
    
    /**
     * Resolve tier from multiple authorities.
     */
    private fun resolveTier(localPro: Boolean, isAdmin: Boolean, serverTier: AccessTier?, user: com.google.firebase.auth.FirebaseUser?): AccessTier {
        // IMMEDIATE ADMIN BYPASS: If email is in hardcoded list, ignore everything else
        val email = user?.email?.lowercase()?.trim()
        if (email == "ivaylozhivkov14@gmail.com" || email == "condignia@gmail.com") {
            return AccessTier.ADMIN
        }

        if (isAdmin || serverTier == AccessTier.ADMIN) return AccessTier.ADMIN
        
        if (TestModeFlag.BYPASS_PAYWALL_IN_DEBUG) return AccessTier.PRO
        
        // Either server says PRO or local billing says PRO
        if (serverTier == AccessTier.PRO || localPro) return AccessTier.PRO
        
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
