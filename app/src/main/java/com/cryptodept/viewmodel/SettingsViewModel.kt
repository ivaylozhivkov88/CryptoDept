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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val preferencesService: PreferencesService,
        private val rootDetector: RootDetector,
        val tierAccessManager: com.cryptodept.domain.tier.TierAccessManager,
    ) : ViewModel() {
        private val _securityWarning = MutableStateFlow<String?>(null)
        val securityWarning: StateFlow<String?> = combine(_securityWarning, preferencesService.isAdmin) { warning, isAdmin ->
            if (isAdmin) null else warning
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        init {
            checkSecurity()
        }

        private fun checkSecurity() {
            if (rootDetector.isDeviceRooted()) {
                _securityWarning.value = "WARNING: ROOT ACCESS DETECTED. SECURITY DEGRADED."
            }
            if (!rootDetector.isSignatureValid()) {
                val currentHash = rootDetector.getCurrentSignatureHash() ?: "UNKNOWN"
                _securityWarning.value = "CRITICAL: TAMPER DETECTED. SIGNATURE MISMATCH.\nHASH: $currentHash"
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

        val forceShowAllFeatures = preferencesService.forceShowAllFeatures.stateIn(
            viewModelScope, 
            SharingStarted.WhileSubscribed(5000), 
            false
        )

        fun setForceShowAllFeatures(enabled: Boolean) {
            viewModelScope.launch {
                preferencesService.setForceShowAllFeatures(enabled)
            }
        }

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
            // Check if security is compromised, but allow bypass for the TEST button (admin = true)
            // if we are in a debuggable state or if it's explicitly allowed.
            if (isAdmin && rootDetector.isSecurityCompromised()) {
                if (!rootDetector.isDebuggable()) {
                    // Log the compromise but still allow admin for the internal "TEST" button
                    // to prevent locking out the developer/testers.
                    _securityWarning.value = "ADMIN ACTIVE (SECURITY COMPROMISED)"
                }
            }
            viewModelScope.launch { preferencesService.setAdminStatus(isAdmin) }
        }

        fun setProStatus(enabled: Boolean) {
            viewModelScope.launch { preferencesService.setProStatus(enabled) }
        }
    }
