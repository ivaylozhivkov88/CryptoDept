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
        serverTierFlow,
        authService.currentUser
    ) { localPro, serverTier, user ->
        val tier = resolveTier(localPro = localPro, serverTier = serverTier, user = user)
        subscribeToSessionTopics(tier)
        tier
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = resolveTier(
            localPro = billingService.isPro.value, 
            serverTier = null,
            user = authService.currentUser.value
        ),
    )

    private fun subscribeToSessionTopics(tier: AccessTier) {
        val tierSuffix = if (tier.canAccess(AccessTier.PRO)) "PRO" else "FREE"
        val oppositeSuffix = if (tier.canAccess(AccessTier.PRO)) "FREE" else "PRO"
        
        val sessions = listOf("LONDON_OPEN", "NY_OPEN", "DAILY_REVIEW")
        val messaging = com.google.firebase.messaging.FirebaseMessaging.getInstance()
        
        sessions.forEach { session ->
            messaging.unsubscribeFromTopic("session_${session}_${oppositeSuffix}")
            messaging.subscribeToTopic("session_${session}_${tierSuffix}")
        }
    }
    
    private fun resolveTier(localPro: Boolean, serverTier: AccessTier?, user: com.google.firebase.auth.FirebaseUser?): AccessTier {
        val email = user?.email?.lowercase()?.trim()
        val adminEmails = setOf(
            "ivaylozhivkov14@gmail.com",
            "condignia@gmail.com",
            "test-reviewer@cryptodept.com"
        )
        
        // ADMIN status is strictly bound to email identity
        if (email != null && email in adminEmails) {
            return AccessTier.ADMIN
        }

        // Secondary check via server-side flag (if identity verified)
        if (serverTier == AccessTier.ADMIN) return AccessTier.ADMIN
        
        // PRO status depends on billing or server confirmation
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
