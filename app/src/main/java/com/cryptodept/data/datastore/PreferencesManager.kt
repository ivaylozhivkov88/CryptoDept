package com.cryptodept.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.io.IOException

// Дефиниция на DataStore
val Context.dataStore by preferencesDataStore(name = "settings")

class PreferencesManager(context: Context) {

    // Използваме applicationContext, за да избегнем memory leaks
    private val dataStore = context.applicationContext.dataStore

    companion object {
        val REFRESH_INTERVAL = intPreferencesKey("refresh_interval")
        val PHOSPHOR_MODE = stringPreferencesKey("phosphor_mode")
        val SOUNDS_ENABLED = booleanPreferencesKey("sounds_enabled")
        val SOUNDS_VOLUME = floatPreferencesKey("sounds_volume")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val SCREENSAVER_TIMEOUT = intPreferencesKey("screensaver_timeout_min")
        val IS_PRO = booleanPreferencesKey("is_pro")
        val LAST_REVIEW_PROMPT_TIME = longPreferencesKey("last_review_prompt_time")
        val LAUNCH_COUNT = intPreferencesKey("launch_count")
    }

    // Flows за четене с вградена защита от грешки
    val refreshInterval: Flow<Int> = dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { it[REFRESH_INTERVAL] ?: 30 }

    val phosphorMode: Flow<String> = dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { it[PHOSPHOR_MODE] ?: "GREEN" }

    val soundsEnabled: Flow<Boolean> = dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { it[SOUNDS_ENABLED] ?: true }

    val soundsVolume: Flow<Float> = dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { it[SOUNDS_VOLUME] ?: 0.5f }

    val notificationsEnabled: Flow<Boolean> = dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { it[NOTIFICATIONS_ENABLED] ?: true }

    val isOnboardingComplete: Flow<Boolean> = dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { it[ONBOARDING_COMPLETE] ?: false }

    val hapticEnabled: Flow<Boolean> = dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { it[HAPTIC_ENABLED] ?: true }

    val screensaverTimeout: Flow<Int> = dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { it[SCREENSAVER_TIMEOUT] ?: 5 }

    val isPro: Flow<Boolean> = dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { it[IS_PRO] ?: false }

    val launchCount: Flow<Int> = dataStore.data
        .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
        .map { it[LAUNCH_COUNT] ?: 0 }

    // ...existing code...
    suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { it[ONBOARDING_COMPLETE] = complete }
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        dataStore.edit { it[HAPTIC_ENABLED] = enabled }
    }

    suspend fun setScreensaverTimeout(minutes: Int) {
        dataStore.edit { it[SCREENSAVER_TIMEOUT] = minutes }
    }

    suspend fun setRefreshInterval(seconds: Int) {
        dataStore.edit { it[REFRESH_INTERVAL] = seconds }
    }

    suspend fun setPhosphorMode(mode: String) {
        dataStore.edit { it[PHOSPHOR_MODE] = mode }
    }

    suspend fun setSoundsEnabled(enabled: Boolean) {
        dataStore.edit { it[SOUNDS_ENABLED] = enabled }
    }

    suspend fun setSoundsVolume(volume: Float) {
        dataStore.edit { it[SOUNDS_VOLUME] = volume }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setProStatus(isPro: Boolean) {
        dataStore.edit { it[IS_PRO] = isPro }
    }

    suspend fun saveLastReviewPromptTime(timestamp: Long) {
        dataStore.edit { it[LAST_REVIEW_PROMPT_TIME] = timestamp }
    }

    suspend fun getLastReviewPromptTime(): Long {
        val prefs = dataStore.data.catch { emit(emptyPreferences()) }.first()
        return prefs[LAST_REVIEW_PROMPT_TIME] ?: 0L
    }

    suspend fun incrementLaunchCount() {
        val prefs = dataStore.data.catch { emit(emptyPreferences()) }.first()
        val currentCount = prefs[LAUNCH_COUNT] ?: 0
        dataStore.edit { it[LAUNCH_COUNT] = currentCount + 1 }
    }

    suspend fun getLaunchCount(): Int {
        val prefs = dataStore.data.catch { emit(emptyPreferences()) }.first()
        return prefs[LAUNCH_COUNT] ?: 0
    }
}