package com.cryptodept.data.remoteconfig

import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigManager @Inject constructor() {

    private val remoteConfig = Firebase.remoteConfig

    init {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        
        // Set default values
        val defaults = mapOf(
            "terminal_amber_force" to false,
            "news_source_url" to "https://cryptocurrency.cv/api/news",
            "min_app_version" to 1
        )
        remoteConfig.setDefaultsAsync(defaults)
    }

    fun fetchAndActivate(onComplete: (Boolean) -> Unit) {
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            onComplete(task.isSuccessful)
        }
    }

    fun getAmberForce(): Boolean = remoteConfig.getBoolean("terminal_amber_force")
    fun getNewsUrl(): String = remoteConfig.getString("news_source_url")
}
