package com.cryptodept.data.datastore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages onboarding flow, tutorial state, and user engagement lifecycle.
 * No knowledge of subscriptions or system settings.
 */
interface UserSessionManager {
    val isOnboardingComplete: Flow<Boolean>
    val isTutorialCompleted: StateFlow<Boolean>
    val launchCount: Flow<Int>

    suspend fun setOnboardingComplete(complete: Boolean)
    suspend fun setTutorialCompleted(completed: Boolean)
    suspend fun incrementLaunchCount()
    suspend fun getLaunchCount(): Int
    suspend fun saveLastReviewPromptTime(timestamp: Long)
    suspend fun getLastReviewPromptTime(): Long

    // Generic key-value store for engagement tracking
    suspend fun getInt(key: String, default: Int): Int
    suspend fun putInt(key: String, value: Int)
    suspend fun getString(key: String, default: String?): String?
    suspend fun putString(key: String, value: String)
    suspend fun getLong(key: String, default: Long): Long
    suspend fun putLong(key: String, value: Long)
    suspend fun getBoolean(key: String, default: Boolean): Boolean
    suspend fun putBoolean(key: String, value: Boolean)
}
