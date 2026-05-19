package com.cryptodept.domain.tier

/**
 * Represents user's effective access tier.
 * 
 * Hierarchy (lowest to highest):
 *   FREE → PRO → ADMIN
 * 
 * - FREE: Default for all users without subscription
 * - PRO: Active Google Play subscription (any plan: day/3day/week/month/quarter/year)
 * - ADMIN: Logged in with hardcoded admin email
 * 
 * Tier is determined by combination of:
 *   - BillingService.isPro (Google Play subscription state)
 *   - PreferencesService.isAdmin (email-based, set during Google Sign-In)
 */
enum class AccessTier(
    val displayName: String,
    val emoji: String,
    val priority: Int,
) {
    FREE("Free", "🆓", priority = 0),
    PRO("Pro", "💎", priority = 1),
    ADMIN("Admin", "🛡️", priority = 2);
    
    /**
     * Check if this tier has access to features that require [required] tier.
     * 
     * Examples:
     *   AccessTier.PRO.canAccess(AccessTier.FREE)  → true
     *   AccessTier.PRO.canAccess(AccessTier.PRO)   → true
     *   AccessTier.PRO.canAccess(AccessTier.ADMIN) → false
     *   AccessTier.ADMIN.canAccess(AccessTier.PRO) → true
     */
    fun canAccess(required: AccessTier): Boolean {
        return this.priority >= required.priority
    }
    
    /**
     * Convenience: is this tier paid (Pro or Admin)?
     */
    val isPaid: Boolean
        get() = priority >= PRO.priority
    
    /**
     * Convenience: is this tier admin?
     */
    val isAdmin: Boolean
        get() = this == ADMIN
}
