package com.cryptodept.data.datastore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Single source of truth for subscription tier, admin privileges, and AI usage limits.
 * No knowledge of UI settings or user session state.
 */
interface SubscriptionAccessManager {
    val isPro: StateFlow<Boolean>
    val isAdmin: StateFlow<Boolean>

    fun isAdmin(): Boolean
    fun getAdminStatusFlow(): Flow<Boolean>

    fun setProStatus(isPro: Boolean)
    fun setAdminStatus(isAdmin: Boolean)
    fun setProExpiry(durationDays: Int)
    fun checkProStatus()
    
    fun setLastBillingCheck(timestamp: Long)
    fun getLastBillingCheck(): Long

    suspend fun getAiReportsCountToday(): Int
    suspend fun incrementAiReportsCount()
}
