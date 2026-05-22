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

@Singleton
class TierAccessManagerImpl @Inject constructor(
    private val billingService: BillingService,
    private val subscription: SubscriptionAccessManager,
    private val firebaseDataSource: com.cryptodept.data.remote.source.FirebaseRemoteDataSource,
    private val authService: com.cryptodept.data.auth.AuthService,
) : TierAccessManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @OptIn(ExperimentalCoroutinesApi::class)
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
    
    override val currentTier: StateFlow<AccessTier> = combine(
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
            isAdmin = subscription.checkIsAdmin(),
            serverTier = null,
            user = authService.currentUser.value
        ),
    )
    
    private fun resolveTier(localPro: Boolean, isAdmin: Boolean, serverTier: AccessTier?, user: com.google.firebase.auth.FirebaseUser?): AccessTier {
        val email = user?.email?.lowercase()?.trim()
        if (email == "ivaylozhivkov14@gmail.com" || email == "condignia@gmail.com") {
            return AccessTier.ADMIN
        }

        if (isAdmin || serverTier == AccessTier.ADMIN) return AccessTier.ADMIN
        
        if (TestModeFlag.BYPASS_PAYWALL_IN_DEBUG) return AccessTier.PRO
        
        if (serverTier == AccessTier.PRO || localPro) return AccessTier.PRO
        
        return AccessTier.FREE
    }
    
    override fun getCachedTier(): AccessTier = currentTier.value
    
    override fun hasAccess(feature: FeatureKey): Boolean {
        return currentTier.value.canAccess(feature.requiredTier)
    }
    
    override fun hasAccessFlow(feature: FeatureKey): Flow<Boolean> {
        return currentTier.map { it.canAccess(feature.requiredTier) }
    }
    
    override fun dumpState(): String = buildString {
        appendLine("=== TierAccessManager State ===")
        appendLine("Current tier: ${currentTier.value}")
        appendLine("Is Pro (raw): ${billingService.isPro.value}")
        appendLine("Is Admin (raw): ${subscription.checkIsAdmin()}")
        appendLine("Test period: ${TestModeFlag.IS_TEST_PERIOD}")
        appendLine("Bypass paywall (debug): ${TestModeFlag.BYPASS_PAYWALL_IN_DEBUG}")
        appendLine("===")
    }
}
