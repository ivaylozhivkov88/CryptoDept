package com.cryptodept.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PROMPT #201 — Secure storage for sensitive information.
 * Wraps EncryptedSharedPreferences for easy access.
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

        private val sharedPreferences: SharedPreferences =
            EncryptedSharedPreferences.create(
                context,
                "secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )

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
