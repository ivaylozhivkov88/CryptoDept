package com.cryptodept.data.remoteconfig

import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigService @Inject constructor() {
    private val remoteConfig = Firebase.remoteConfig

    init {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600
        }
        remoteConfig.setConfigSettingsAsync(configSettings)

        // Set default values matching Firebase Console
        val defaults = mapOf(
            "min_version_code" to 14L,
            "free_ai_limit_daily" to 2L,
            "terminal_broadcast_msg" to "",
            "whale_usd_threshold" to 500000L,
            "pro_sale_active" to false,
            "gemini_model_name" to "gemini-1.5-flash"
        )
        remoteConfig.setDefaultsAsync(defaults)
    }

    fun fetchAndActivate(onComplete: (Boolean) -> Unit) {
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            onComplete(task.isSuccessful)
        }
    }

    fun getMinVersionCode(): Int = remoteConfig.getLong("min_version_code").toInt()

    fun getFreeAiLimitDaily(): Int = remoteConfig.getLong("free_ai_limit_daily").toInt()

    fun getTerminalBroadcastMsg(): String = remoteConfig.getString("terminal_broadcast_msg")

    fun getWhaleUsdThreshold(): Double = remoteConfig.getLong("whale_usd_threshold").toDouble()

    fun isProSaleActive(): Boolean = remoteConfig.getBoolean("pro_sale_active")

    fun getGeminiModel(): String = remoteConfig.getString("gemini_model_name")
}
