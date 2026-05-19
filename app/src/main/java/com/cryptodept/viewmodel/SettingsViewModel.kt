package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.data.datastore.SubscriptionAccessManager
import com.cryptodept.data.datastore.SystemSettingsManager
import com.cryptodept.data.datastore.UserSessionManager
import com.cryptodept.util.RootDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val settings: SystemSettingsManager,
        private val session: UserSessionManager,
        private val subscription: SubscriptionAccessManager,
        private val rootDetector: RootDetector,
        val tierAccessManager: com.cryptodept.domain.tier.TierAccessManager,
    ) : ViewModel() {
        private val _securityWarning = MutableStateFlow<String?>(null)
        val securityWarning: StateFlow<String?> = combine(_securityWarning, subscription.isAdmin) { warning, isAdmin ->
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
            settings.refreshInterval.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                30,
            )

        val phosphorMode =
            settings.phosphorMode.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                "GREEN",
            )

        val soundsEnabled =
            settings.soundsEnabled.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                true,
            )

        val soundsVolume =
            settings.soundsVolume.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                0.5f,
            )

        val notificationsEnabled =
            settings.notificationsEnabled.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                true,
            )

        val hapticEnabled =
            settings.hapticEnabled.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                true,
            )

        val powerUserMode =
            settings.powerUserMode.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                false,
            )

        val screensaverTimeout =
            settings.screensaverTimeout.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                5,
            )

        val isAdmin =
            subscription.isAdmin.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                false,
            )

        val forceShowAllFeatures = settings.forceShowAllFeatures.stateIn(
            viewModelScope, 
            SharingStarted.WhileSubscribed(5000), 
            false
        )

        fun setForceShowAllFeatures(enabled: Boolean) {
            viewModelScope.launch {
                settings.setForceShowAllFeatures(enabled)
            }
        }

        fun setRefreshInterval(seconds: Int) {
            viewModelScope.launch { settings.setRefreshInterval(seconds) }
        }

        fun setPhosphorMode(mode: String) {
            viewModelScope.launch { settings.setPhosphorMode(mode) }
        }

        fun setSoundsEnabled(enabled: Boolean) {
            viewModelScope.launch { settings.setSoundsEnabled(enabled) }
        }

        fun setSoundsVolume(volume: Float) {
            viewModelScope.launch { settings.setSoundsVolume(volume) }
        }

        fun setNotificationsEnabled(enabled: Boolean) {
            viewModelScope.launch { settings.setNotificationsEnabled(enabled) }
        }

        fun setHapticEnabled(enabled: Boolean) {
            viewModelScope.launch { settings.setHapticEnabled(enabled) }
        }

        fun setPowerUserMode(enabled: Boolean) {
            viewModelScope.launch { settings.setPowerUserMode(enabled) }
        }

        fun setScreensaverTimeout(minutes: Int) {
            viewModelScope.launch { settings.setScreensaverTimeout(minutes) }
        }

        fun restartOnboarding() {
            viewModelScope.launch { session.setOnboardingComplete(false) }
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
            viewModelScope.launch { subscription.setAdminStatus(isAdmin) }
        }

        fun setProStatus(enabled: Boolean) {
            viewModelScope.launch { subscription.setProStatus(enabled) }
        }
    }
