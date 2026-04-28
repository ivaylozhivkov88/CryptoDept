package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.data.datastore.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val refreshInterval = preferencesManager.refreshInterval.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 30
    )

    val phosphorMode = preferencesManager.phosphorMode.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "GREEN"
    )

    val soundsEnabled = preferencesManager.soundsEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    val soundsVolume = preferencesManager.soundsVolume.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 0.5f
    )

    val notificationsEnabled = preferencesManager.notificationsEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    fun setRefreshInterval(seconds: Int) {
        viewModelScope.launch { preferencesManager.setRefreshInterval(seconds) }
    }

    fun setPhosphorMode(mode: String) {
        viewModelScope.launch { preferencesManager.setPhosphorMode(mode) }
    }

    fun setSoundsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setSoundsEnabled(enabled) }
    }

    fun setSoundsVolume(volume: Float) {
        viewModelScope.launch { preferencesManager.setSoundsVolume(volume) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setNotificationsEnabled(enabled) }
    }
}