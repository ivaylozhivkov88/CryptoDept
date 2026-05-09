package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.data.datastore.PreferencesService
import com.cryptodept.util.RootDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val preferencesService: PreferencesService,
        private val rootDetector: RootDetector,
    ) : ViewModel() {
        private val _securityWarning = MutableStateFlow<String?>(null)
        val securityWarning: StateFlow<String?> = _securityWarning.asStateFlow()

        init {
            checkSecurity()
        }

        private fun checkSecurity() {
            if (rootDetector.isDeviceRooted()) {
                _securityWarning.value = "WARNING: ROOT ACCESS DETECTED. SECURITY DEGRADED."
            }
            if (!rootDetector.isSignatureValid()) {
                _securityWarning.value = "CRITICAL: TAMPER DETECTED. UNAUTHORIZED APK SIGNATURE."
            }
        }

        val refreshInterval =
            preferencesService.refreshInterval.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                30,
            )

        val phosphorMode =
            preferencesService.phosphorMode.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                "GREEN",
            )

        val soundsEnabled =
            preferencesService.soundsEnabled.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                true,
            )

        val soundsVolume =
            preferencesService.soundsVolume.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                0.5f,
            )

        val notificationsEnabled =
            preferencesService.notificationsEnabled.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                true,
            )

        val hapticEnabled =
            preferencesService.hapticEnabled.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                true,
            )

        val powerUserMode =
            preferencesService.powerUserMode.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                false,
            )

        val screensaverTimeout =
            preferencesService.screensaverTimeout.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                5,
            )

        val isAdmin =
            preferencesService.isAdmin.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                false,
            )

        fun setRefreshInterval(seconds: Int) {
            viewModelScope.launch { preferencesService.setRefreshInterval(seconds) }
        }

        fun setPhosphorMode(mode: String) {
            viewModelScope.launch { preferencesService.setPhosphorMode(mode) }
        }

        fun setSoundsEnabled(enabled: Boolean) {
            viewModelScope.launch { preferencesService.setSoundsEnabled(enabled) }
        }

        fun setSoundsVolume(volume: Float) {
            viewModelScope.launch { preferencesService.setSoundsVolume(volume) }
        }

        fun setNotificationsEnabled(enabled: Boolean) {
            viewModelScope.launch { preferencesService.setNotificationsEnabled(enabled) }
        }

        fun setHapticEnabled(enabled: Boolean) {
            viewModelScope.launch { preferencesService.setHapticEnabled(enabled) }
        }

        fun setPowerUserMode(enabled: Boolean) {
            viewModelScope.launch { preferencesService.setPowerUserMode(enabled) }
        }

        fun setScreensaverTimeout(minutes: Int) {
            viewModelScope.launch { preferencesService.setScreensaverTimeout(minutes) }
        }

        fun restartOnboarding() {
            viewModelScope.launch { preferencesService.setOnboardingComplete(false) }
        }

        fun setAdminStatus(isAdmin: Boolean) {
            if (isAdmin && rootDetector.isSecurityCompromised()) {
                // Block admin in production if compromised
                if (!rootDetector.isDebuggable()) {
                    _securityWarning.value = "ADMIN ACCESS BLOCKED ON COMPROMISED DEVICE."
                    return
                }
            }
            viewModelScope.launch { preferencesService.setAdminStatus(isAdmin) }
        }
    }
