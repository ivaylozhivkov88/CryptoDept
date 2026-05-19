package com.cryptodept.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PROMPT #201 — Secure storage for sensitive information.
 * Wraps EncryptedSharedPreferences with crash protection for Keystore mismatches.
 */
@Singleton
class SecurePrefsService
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val masterKey =
            MasterKey
                .Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

        private val sharedPreferences: SharedPreferences = try {
            createEncryptedPrefs(context, masterKey)
        } catch (e: Exception) {
            Log.e("SecurePrefs", "Failed to initialize encrypted prefs, possible Keystore mismatch. Clearing...", e)
            // If creation fails (e.g. AEADBadTagException), clear the file and retry once
            context.getSharedPreferences("secure_prefs", Context.MODE_PRIVATE).edit().clear().apply()
            try {
                createEncryptedPrefs(context, masterKey)
            } catch (e2: Exception) {
                Log.e("SecurePrefs", "Fatal failure to initialize secure storage", e2)
                // Fallback to plain prefs if encryption is completely broken on device
                context.getSharedPreferences("secure_prefs_fallback", Context.MODE_PRIVATE)
            }
        }

        private fun createEncryptedPrefs(context: Context, key: MasterKey): SharedPreferences {
            return EncryptedSharedPreferences.create(
                context,
                "secure_prefs",
                key,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }

        fun saveString(
            key: String,
            value: String,
        ) {
            sharedPreferences.edit().putString(key, value).apply()
        }

        fun getString(
            key: String,
            defaultValue: String? = null,
        ): String? = sharedPreferences.getString(key, defaultValue)

        fun saveBoolean(
            key: String,
            value: Boolean,
        ) {
            sharedPreferences.edit().putBoolean(key, value).apply()
        }

        fun getBoolean(
            key: String,
            defaultValue: Boolean = false,
        ): Boolean = sharedPreferences.getBoolean(key, defaultValue)

        fun saveLong(
            key: String,
            value: Long,
        ) {
            sharedPreferences.edit().putLong(key, value).apply()
        }

        fun getLong(
            key: String,
            defaultValue: Long = 0L,
        ): Long = sharedPreferences.getLong(key, defaultValue)

        fun remove(key: String) {
            sharedPreferences.edit().remove(key).apply()
        }

        fun clear() {
            sharedPreferences.edit().clear().apply()
        }

        fun getDatabasePassword(): ByteArray {
            var password = getString(KEY_DB_PASSPHRASE)
            if (password == null) {
                password =
                    java.util.UUID
                        .randomUUID()
                        .toString()
                saveString(KEY_DB_PASSPHRASE, password)
            }
            return password.toByteArray()
        }

        companion object {
            const val KEY_USER_API_KEY = "user_api_key"
            const val KEY_ADMIN_PASSWORD = "admin_password"
            const val KEY_SUBSCRIPTION_STATE = "subscription_state"
            const val KEY_DB_PASSPHRASE = "db_passphrase"
        }
    }
