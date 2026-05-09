package com.cryptodept.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.cryptodept.util.SecurePrefsService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// Дефиниция на DataStore
val Context.dataStore by preferencesDataStore(name = "settings")

typealias PreferencesManager = PreferencesService

@Singleton
class PreferencesService
    @Inject
    constructor(
        @dagger.hilt.android.qualifiers.ApplicationContext context: Context,
        private val securePrefs: SecurePrefsService,
    ) {
        // Използваме applicationContext, за да избегнем memory leaks
        private val dataStore = context.applicationContext.dataStore
        private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

        init {
            scope.launch {
                migrateIfNeeded()
            }
        }

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
            val IS_ADMIN = booleanPreferencesKey("is_admin")
            val POWER_USER_MODE = booleanPreferencesKey("power_user_mode")
            val FOCUS_MODE_ENABLED = booleanPreferencesKey("focus_mode_enabled")
            val LAST_REVIEW_PROMPT_TIME = longPreferencesKey("last_review_prompt_time")
            val LAUNCH_COUNT = intPreferencesKey("launch_count")

            const val KEY_MIGRATED_TO_SECURE = "migrated_to_secure_v5"
        }

        // Flows за четене с вградена защита от грешки
        val refreshInterval: Flow<Int> =
            dataStore.data
                .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
                .map { it[REFRESH_INTERVAL] ?: 30 }

        val phosphorMode: Flow<String> =
            dataStore.data
                .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
                .map { it[PHOSPHOR_MODE] ?: "GREEN" }

        val soundsEnabled: Flow<Boolean> =
            dataStore.data
                .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
                .map { it[SOUNDS_ENABLED] ?: true }

        val soundsVolume: Flow<Float> =
            dataStore.data
                .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
                .map { it[SOUNDS_VOLUME] ?: 0.5f }

        val notificationsEnabled: Flow<Boolean> =
            dataStore.data
                .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
                .map { it[NOTIFICATIONS_ENABLED] ?: true }

        val isOnboardingComplete: Flow<Boolean> =
            dataStore.data
                .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
                .map { it[ONBOARDING_COMPLETE] ?: false }

        val hapticEnabled: Flow<Boolean> =
            dataStore.data
                .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
                .map { it[HAPTIC_ENABLED] ?: true }

        val screensaverTimeout: Flow<Int> =
            dataStore.data
                .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
                .map { it[SCREENSAVER_TIMEOUT] ?: 1 }

        val powerUserMode: Flow<Boolean> =
            dataStore.data
                .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
                .map { it[POWER_USER_MODE] ?: false }

        val focusModeEnabled: Flow<Boolean> =
            dataStore.data
                .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
                .map { it[FOCUS_MODE_ENABLED] ?: false }

        val isPro: Flow<Boolean> =
            flow {
                val secureValue = securePrefs.getBoolean("is_pro", false)
                emit(secureValue)
            }

        val isAdmin: Flow<Boolean> =
            flow {
                val secureValue = securePrefs.getBoolean("is_admin", false)
                emit(secureValue)
            }

        suspend fun performMigration() {
            migrateIfNeeded()
        }

        val launchCount: Flow<Int> =
            dataStore.data
                .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
                .map { it[LAUNCH_COUNT] ?: 0 }

        private suspend fun migrateIfNeeded() {
            val prefs = dataStore.data.first()
            val isMigrated = prefs[booleanPreferencesKey(KEY_MIGRATED_TO_SECURE)] ?: false

            if (!isMigrated) {
                val oldPro = oldPro(prefs)
                val oldAdmin = oldAdmin(prefs)

                securePrefs.saveBoolean("is_pro", oldPro)
                securePrefs.saveBoolean("is_admin", oldAdmin)

                dataStore.edit {
                    it[booleanPreferencesKey(KEY_MIGRATED_TO_SECURE)] = true
                    it.remove(IS_PRO)
                    it.remove(IS_ADMIN)
                }
            }
        }

        private fun oldPro(prefs: Preferences): Boolean = prefs[IS_PRO] ?: false

        private fun oldAdmin(prefs: Preferences): Boolean = prefs[IS_ADMIN] ?: false

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

        fun setProStatus(isPro: Boolean) {
            securePrefs.saveBoolean("is_pro", isPro)
        }

        fun setAdminStatus(isAdmin: Boolean) {
            securePrefs.saveBoolean("is_admin", isAdmin)
        }

        suspend fun setPowerUserMode(enabled: Boolean) {
            dataStore.edit { it[POWER_USER_MODE] = enabled }
        }

        suspend fun setFocusModeEnabled(enabled: Boolean) {
            dataStore.edit { it[FOCUS_MODE_ENABLED] = enabled }
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
