package com.cryptodept.domain.update

/**
 * Represents the state of app updates from Google Play.
 */
sealed class AppUpdateState {
    /**
     * Initial state — checking with Play Store.
     */
    object Checking : AppUpdateState()
    
    /**
     * No update available — app is on latest version.
     */
    object UpToDate : AppUpdateState()
    
    /**
     * Update is available.
     * @param availableVersionCode New version in Play Store
     * @param updatePriority From Play Console (0-5, higher = more critical)
     * @param isImmediate If true, update is critical and should block app usage
     * @param isFlexible If true, can be downloaded in background
     */
    data class Available(
        val availableVersionCode: Int,
        val updatePriority: Int,
        val isImmediate: Boolean,
        val isFlexible: Boolean,
        val sizeBytes: Long,
    ) : AppUpdateState()
    
    /**
     * Update is being downloaded in background (flexible update only).
     * @param bytesDownloaded Current progress
     * @param totalBytes Total size
     */
    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytes: Long,
    ) : AppUpdateState() {
        val progressPercent: Int
            get() = if (totalBytes > 0) ((bytesDownloaded.toFloat() / totalBytes) * 100).toInt() else 0
    }
    
    /**
     * Update downloaded, waiting for user to confirm install + restart.
     */
    object DownloadedReadyToInstall : AppUpdateState()
    
    /**
     * Error occurred during update check or download.
     */
    data class Error(val message: String) : AppUpdateState()
}

/**
 * Update priority levels (controlled from Play Console).
 * Higher number = more aggressive prompting.
 */
object UpdatePriority {
    const val MINIMAL = 0   // Just show update notice
    const val LOW = 1
    const val MEDIUM = 2
    const val HIGH = 3
    const val URGENT = 4
    const val CRITICAL = 5  // Force immediate update — security/breaking
}
