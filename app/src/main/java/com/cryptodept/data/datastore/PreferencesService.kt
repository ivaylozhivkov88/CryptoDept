package com.cryptodept.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.cryptodept.util.SecurePrefsService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
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
    ) : SystemSettingsManager, UserSessionManager, SubscriptionAccessManager {
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
            val PROMO_SNACKBAR_SHOWN = booleanPreferencesKey("promo_snackbar_shown")
            val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
            val SCREENSAVER_TIMEOUT = intPreferencesKey("screensaver_timeout_min")
            val IS_PRO = booleanPreferencesKey("is_pro")
            val IS_ADMIN = booleanPreferencesKey("is_admin")
            val POWER_USER_MODE = booleanPreferencesKey("power_user_mode")
            val FOCUS_MODE_ENABLED = booleanPreferencesKey("focus_mode_enabled")
            val CRASHLYTICS_CONSENT = booleanPreferencesKey("crashlytics_consent")
            val TUTORIAL_COMPLETED = booleanPreferencesKey("tutorial_completed_v1")
            val LAST_REVIEW_PROMPT_TIME = longPreferencesKey("last_review_prompt_time")
            val LAUNCH_COUNT = intPreferencesKey("launch_count")
            val PRO_EXPIRY_TIMESTAMP = longPreferencesKey("pro_expiry_timestamp")
            val LAST_BILLING_CHECK = longPreferencesKey("last_billing_check")
            val AI_REPORTS_COUNT = intPreferencesKey("ai_reports_count")
            val LAST_AI_REPORT_DATE = stringPreferencesKey("last_ai_report_date")
            val TILT_PROTECTION_ENABLED = booleanPreferencesKey("tilt_protection_enabled")

            const val KEY_MIGRATED_TO_SECURE = "migrated_to_secure_v5"
        }

        // Flows за четене с вградена защита от грешки
        private val _isPro = MutableStateFlow(securePrefs.getBoolean("is_pro", false) || (securePrefs.getLong("pro_expiry_timestamp", 0L) > System.currentTimeMillis()))
        override val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

        private val _isAdmin = MutableStateFlow(securePrefs.getBoolean("is_admin", false))
        override val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

        private val _isTutorialCompleted = MutableStateFlow(securePrefs.getBoolean("tutorial_completed_v1", false))
        override val isTutorialCompleted: StateFlow<Boolean> = _isTutorialCompleted.asStateFlow()

        override val refreshInterval: Flow<Int> =
            dataStore.data
                .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
                .map { it[REFRESH_INTERVAL] ?: 30 }

        override val phosphorMode: Flow<String> =
            dataStore.data
                .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
                .map { it[PHOSPHOR_MODE] ?: "GREEN" }

        override val soundsEnabled: Flow<Boolean> =
            dataStore.data
                .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
                .map { it[SOUNDS_ENABLED] ?: false }

        override val soundsVolume: Flow<Float> =
            dataStore.data
                .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
                .map { it[SOUNDS_VOLUME] ?: 0.5f }

        override val notificationsEnabled: Flow<Boolean> =
            dataStore.data
                .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
                .map { it[NOTIFICATIONS_ENABLED] ?: true }

        override val isOnboardingComplete: Flow<Boolean> =
            dataStore.data
                .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
                .map { it[ONBOARDING_COMPLETE] ?: false }

        override val hapticEnabled: Flow<Boolean> =
            dataStore.data
                .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
                .map { it[HAPTIC_ENABLED] ?: true }

        override val screensaverTimeout: Flow<Int> =
            dataStore.data
                .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
                .map { it[SCREENSAVER_TIMEOUT] ?: 1 }

        override val powerUserMode: Flow<Boolean> =
            dataStore.data
                .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
                .map { it[POWER_USER_MODE] ?: false }

        override val focusModeEnabled: Flow<Boolean> =
            dataStore.data
                .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
                .map { it[FOCUS_MODE_ENABLED] ?: false }

        override val crashlyticsConsent: Flow<Boolean> = dataStore.data.map { it[CRASHLYTICS_CONSENT] ?: true }

        suspend fun performMigration() {
            migrateIfNeeded()
        }

        override val launchCount: Flow<Int> =
            dataStore.data
                .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
                .map { it[LAUNCH_COUNT] ?: 0 }

        override val forceShowAllFeatures: Flow<Boolean> =
            dataStore.data
                .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
                .map { it[booleanPreferencesKey("force_show_all_features")] ?: false }

        override val tiltProtectionEnabled: Flow<Boolean> =
            dataStore.data
                .catch { exception -> if (exception is IOException) emit(emptyPreferences()) else throw exception }
                .map { it[TILT_PROTECTION_ENABLED] ?: true }

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

        override suspend fun setTiltProtectionEnabled(enabled: Boolean) {
            dataStore.edit { it[TILT_PROTECTION_ENABLED] = enabled }
        }

        override suspend fun setForceShowAllFeatures(enabled: Boolean) {
            dataStore.edit { it[booleanPreferencesKey("force_show_all_features")] = enabled }
        }

        override suspend fun setOnboardingComplete(complete: Boolean) {
            dataStore.edit { it[ONBOARDING_COMPLETE] = complete }
        }

        override suspend fun setHapticEnabled(enabled: Boolean) {
            dataStore.edit { it[HAPTIC_ENABLED] = enabled }
        }

        override suspend fun setScreensaverTimeout(minutes: Int) {
            dataStore.edit { it[SCREENSAVER_TIMEOUT] = minutes }
        }

        override suspend fun setRefreshInterval(seconds: Int) {
            dataStore.edit { it[REFRESH_INTERVAL] = seconds }
        }

        override suspend fun setPhosphorMode(mode: String) {
            dataStore.edit { it[PHOSPHOR_MODE] = mode }
        }

        override suspend fun setSoundsEnabled(enabled: Boolean) {
            dataStore.edit { it[SOUNDS_ENABLED] = enabled }
        }

        override suspend fun setSoundsVolume(volume: Float) {
            dataStore.edit { it[SOUNDS_VOLUME] = volume }
        }

        override suspend fun setNotificationsEnabled(enabled: Boolean) {
            dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
        }

        override fun setProStatus(isPro: Boolean) {
            securePrefs.saveBoolean("is_pro", isPro)
            _isPro.value = isPro
        }

        override fun setAdminStatus(isAdmin: Boolean) {
            securePrefs.saveBoolean("is_admin", isAdmin)
            _isAdmin.value = isAdmin
            // Admins are automatically PRO users
            if (isAdmin) {
                setProStatus(true)
            } else {
                // Task: When revoking Admin, also revoke Pro for testing purposes
                setProStatus(false)
            }
        }

        override fun checkIsAdmin(): Boolean = _isAdmin.value

        override fun getAdminStatusFlow(): Flow<Boolean> = _isAdmin.asStateFlow()

        override fun setProExpiry(durationDays: Int) {
            val currentExpiry = securePrefs.getLong("pro_expiry_timestamp", 0L)
            val startTime = if (currentExpiry > System.currentTimeMillis()) currentExpiry else System.currentTimeMillis()
            val newExpiry = startTime + (durationDays * 24 * 60 * 60 * 1000L)
            
            securePrefs.saveLong("pro_expiry_timestamp", newExpiry)
            _isPro.value = true
            
            scope.launch {
                dataStore.edit { it[PRO_EXPIRY_TIMESTAMP] = newExpiry }
            }
        }

        override fun checkProStatus() {
            val expiry = securePrefs.getLong("pro_expiry_timestamp", 0L)
            val isProByBilling = securePrefs.getBoolean("is_pro", false)
            val isExpired = expiry <= System.currentTimeMillis()
            
            if (!isProByBilling && expiry > 0 && isExpired) {
                _isPro.value = false
            } else if (expiry > System.currentTimeMillis()) {
                _isPro.value = true
            }
        }

        override fun setLastBillingCheck(timestamp: Long) {
            securePrefs.saveLong("last_billing_check", timestamp)
            scope.launch {
                dataStore.edit { it[longPreferencesKey("last_billing_check")] = timestamp }
            }
        }

        override fun getLastBillingCheck(): Long {
            return securePrefs.getLong("last_billing_check", 0L)
        }

        override suspend fun setPowerUserMode(enabled: Boolean) {
            dataStore.edit { it[POWER_USER_MODE] = enabled }
        }

        override suspend fun setFocusModeEnabled(enabled: Boolean) {
            dataStore.edit { it[FOCUS_MODE_ENABLED] = enabled }
        }

        override suspend fun setCrashlyticsConsent(enabled: Boolean) {
            dataStore.edit { it[CRASHLYTICS_CONSENT] = enabled }
        }

        override suspend fun setTutorialCompleted(completed: Boolean) {
            securePrefs.saveBoolean("tutorial_completed_v1", completed)
            _isTutorialCompleted.value = completed
            dataStore.edit { it[TUTORIAL_COMPLETED] = completed }
        }

        override suspend fun saveLastReviewPromptTime(timestamp: Long) {
            dataStore.edit { it[LAST_REVIEW_PROMPT_TIME] = timestamp }
        }

        override suspend fun getLastReviewPromptTime(): Long {
            val prefs = dataStore.data.catch { emit(emptyPreferences()) }.first()
            return prefs[LAST_REVIEW_PROMPT_TIME] ?: 0L
        }

        override suspend fun incrementLaunchCount() {
            val prefs = dataStore.data.catch { emit(emptyPreferences()) }.first()
            val currentCount = prefs[LAUNCH_COUNT] ?: 0
            dataStore.edit { it[LAUNCH_COUNT] = currentCount + 1 }
        }

        override suspend fun getLaunchCount(): Int {
            val prefs = dataStore.data.catch { emit(emptyPreferences()) }.first()
            return prefs[LAUNCH_COUNT] ?: 0
        }

        override suspend fun getAiReportsCountToday(): Int {
            val prefs = dataStore.data.catch { emit(emptyPreferences()) }.first()
            val lastDate = prefs[LAST_AI_REPORT_DATE] ?: ""
            val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date())
            
            return if (lastDate == today) {
                prefs[AI_REPORTS_COUNT] ?: 0
            } else {
                0
            }
        }

        override suspend fun incrementAiReportsCount() {
            val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date())
            val currentCount = getAiReportsCountToday()
            
            dataStore.edit {
                it[AI_REPORTS_COUNT] = currentCount + 1
                it[LAST_AI_REPORT_DATE] = today
            }
        }

        // Generic accessors for engagement tracking
        override suspend fun getInt(key: String, default: Int): Int {
            val prefs = dataStore.data.catch { emit(emptyPreferences()) }.first()
            return prefs[intPreferencesKey(key)] ?: default
        }

        override suspend fun putInt(key: String, value: Int) {
            dataStore.edit { it[intPreferencesKey(key)] = value }
        }

        override suspend fun getBoolean(key: String, default: Boolean): Boolean {
            val prefs = dataStore.data.catch { emit(emptyPreferences()) }.first()
            return prefs[booleanPreferencesKey(key)] ?: default
        }

        override suspend fun putBoolean(key: String, value: Boolean) {
            dataStore.edit { it[booleanPreferencesKey(key)] = value }
        }

        override suspend fun getLong(key: String, default: Long): Long {
            val prefs = dataStore.data.catch { emit(emptyPreferences()) }.first()
            return prefs[longPreferencesKey(key)] ?: default
        }

        override suspend fun putLong(key: String, value: Long) {
            dataStore.edit { it[longPreferencesKey(key)] = value }
        }

        override suspend fun getString(key: String, default: String?): String? {
            val prefs = dataStore.data.catch { emit(emptyPreferences()) }.first()
            return prefs[stringPreferencesKey(key)] ?: default
        }

        override suspend fun putString(key: String, value: String) {
            dataStore.edit { it[stringPreferencesKey(key)] = value }
        }

        suspend fun isPromoSnackbarShown(): Boolean {
            return dataStore.data.map { it[PROMO_SNACKBAR_SHOWN] ?: false }.first()
        }

        suspend fun markPromoSnackbarShown() {
            dataStore.edit { it[PROMO_SNACKBAR_SHOWN] = true }
        }
    }
