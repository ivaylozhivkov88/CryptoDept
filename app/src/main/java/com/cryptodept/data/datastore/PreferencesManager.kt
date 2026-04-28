package com.cryptodept.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
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

    // Методи за запис
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
}