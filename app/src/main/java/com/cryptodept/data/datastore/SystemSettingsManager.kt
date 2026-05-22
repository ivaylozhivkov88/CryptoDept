package com.cryptodept.data.datastore

import kotlinx.coroutines.flow.Flow

/**
 * Manages purely visual and audio system configuration.
 * No knowledge of subscriptions, users, or business logic.
 */
interface SystemSettingsManager {
    val refreshInterval: Flow<Int>
    val phosphorMode: Flow<String>
    val soundsEnabled: Flow<Boolean>
    val soundsVolume: Flow<Float>
    val hapticEnabled: Flow<Boolean>
    val notificationsEnabled: Flow<Boolean>
    val screensaverTimeout: Flow<Int>
    val powerUserMode: Flow<Boolean>
    val focusModeEnabled: Flow<Boolean>
    val crashlyticsConsent: Flow<Boolean>
    val forceShowAllFeatures: Flow<Boolean>
    val tiltProtectionEnabled: Flow<Boolean>

    suspend fun setRefreshInterval(seconds: Int)
    suspend fun setPhosphorMode(mode: String)
    suspend fun setSoundsEnabled(enabled: Boolean)
    suspend fun setSoundsVolume(volume: Float)
    suspend fun setHapticEnabled(enabled: Boolean)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setScreensaverTimeout(minutes: Int)
    suspend fun setPowerUserMode(enabled: Boolean)
    suspend fun setFocusModeEnabled(enabled: Boolean)
    suspend fun setCrashlyticsConsent(enabled: Boolean)
    suspend fun setForceShowAllFeatures(enabled: Boolean)
    suspend fun setTiltProtectionEnabled(enabled: Boolean)
}
