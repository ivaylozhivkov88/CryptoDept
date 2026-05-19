package com.cryptodept.data.update

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.cryptodept.data.datastore.PreferencesService
import com.cryptodept.domain.update.AppUpdateState
import com.cryptodept.domain.update.UpdatePriority
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository wrapping Google Play's AppUpdateManager.
 * 
 * Provides reactive state of app updates and handles:
 * - Update checking (every app launch)
 * - Flexible update download (in-background)
 * - Immediate update (force restart for critical updates)
 * - 24h cooldown on dismiss
 */
@Singleton
class AppUpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: PreferencesService,
) {
    private val appUpdateManager: AppUpdateManager by lazy {
        AppUpdateManagerFactory.create(context)
    }
    
    private val _updateState = MutableStateFlow<AppUpdateState>(AppUpdateState.Checking)
    val updateState: StateFlow<AppUpdateState> = _updateState.asStateFlow()
    
    private var installStateListener: InstallStateUpdatedListener? = null
    
    /**
     * Check for available updates.
     * Respects 24h cooldown if user dismissed recently.
     */
    suspend fun checkForUpdates() {
        // Respect dismiss cooldown
        if (isWithinDismissCooldown()) {
            _updateState.value = AppUpdateState.UpToDate
            return
        }
        
        try {
            _updateState.value = AppUpdateState.Checking
            val appUpdateInfo = appUpdateManager.appUpdateInfo.await()
            
            when (appUpdateInfo.updateAvailability()) {
                UpdateAvailability.UPDATE_AVAILABLE -> {
                    val isImmediate = shouldBeImmediate(appUpdateInfo)
                    val isFlexible = !isImmediate
                    
                    _updateState.value = AppUpdateState.Available(
                        availableVersionCode = appUpdateInfo.availableVersionCode(),
                        updatePriority = appUpdateInfo.updatePriority(),
                        isImmediate = isImmediate,
                        isFlexible = isFlexible,
                        sizeBytes = appUpdateInfo.totalBytesToDownload(),
                    )
                }
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                    // Update is being downloaded
                    _updateState.value = AppUpdateState.Downloading(
                        bytesDownloaded = appUpdateInfo.bytesDownloaded(),
                        totalBytes = appUpdateInfo.totalBytesToDownload(),
                    )
                }
                else -> {
                    _updateState.value = AppUpdateState.UpToDate
                }
            }
        } catch (e: Exception) {
            _updateState.value = AppUpdateState.Error(
                e.message ?: "Update check failed"
            )
        }
    }
    
    /**
     * Determine if update should be immediate (blocking) based on priority.
     * Priority 4-5 = immediate. Priority 0-3 = flexible.
     */
    private fun shouldBeImmediate(info: AppUpdateInfo): Boolean {
        return info.updatePriority() >= UpdatePriority.URGENT &&
               info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
    }
    
    /**
     * Start flexible update — download in background.
     */
    fun startFlexibleUpdate(
        activity: Activity,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
    ) {
        if (_updateState.value !is AppUpdateState.Available) return
        
        registerInstallStateListener()
        
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                val options = AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                appUpdateManager.startUpdateFlowForResult(info, launcher, options)
            }
        }
    }
    
    /**
     * Start immediate update — blocks app usage until install.
     * Use only for critical updates (priority 4-5).
     */
    fun startImmediateUpdate(
        activity: Activity,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
    ) {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                val options = AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                appUpdateManager.startUpdateFlowForResult(info, launcher, options)
            }
        }
    }
    
    /**
     * Complete pending update (called after download finishes).
     * Triggers app restart.
     */
    fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }
    
    /**
     * User dismissed update prompt — set cooldown.
     */
    suspend fun dismissUpdate() {
        preferences.putLong(KEY_DISMISS_TIME, System.currentTimeMillis())
        _updateState.value = AppUpdateState.UpToDate
    }
    
    /**
     * Check if we're within 24h cooldown period.
     */
    private suspend fun isWithinDismissCooldown(): Boolean {
        val dismissTime = preferences.getLong(KEY_DISMISS_TIME, 0L)
        if (dismissTime == 0L) return false
        
        val now = System.currentTimeMillis()
        val cooldownMillis = 24 * 60 * 60 * 1000L
        return (now - dismissTime) < cooldownMillis
    }
    
    /**
     * Register listener to update state during flexible download.
     */
    private fun registerInstallStateListener() {
        if (installStateListener != null) return
        
        installStateListener = InstallStateUpdatedListener { state ->
            when (state.installStatus()) {
                InstallStatus.DOWNLOADING -> {
                    _updateState.value = AppUpdateState.Downloading(
                        bytesDownloaded = state.bytesDownloaded(),
                        totalBytes = state.totalBytesToDownload(),
                    )
                }
                InstallStatus.DOWNLOADED -> {
                    _updateState.value = AppUpdateState.DownloadedReadyToInstall
                }
                InstallStatus.INSTALLED -> {
                    _updateState.value = AppUpdateState.UpToDate
                    unregisterInstallStateListener()
                }
                InstallStatus.FAILED -> {
                    _updateState.value = AppUpdateState.Error("Download failed")
                    unregisterInstallStateListener()
                }
                else -> { /* no-op */ }
            }
        }
        
        appUpdateManager.registerListener(installStateListener!!)
    }
    
    private fun unregisterInstallStateListener() {
        installStateListener?.let {
            appUpdateManager.unregisterListener(it)
        }
        installStateListener = null
    }
    
    companion object {
        private const val KEY_DISMISS_TIME = "app_update_dismiss_time"
    }
}
