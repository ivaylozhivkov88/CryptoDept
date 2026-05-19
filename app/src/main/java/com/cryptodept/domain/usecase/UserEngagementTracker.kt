package com.cryptodept.domain.usecase

import com.cryptodept.data.datastore.PreferencesService
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserEngagementTracker @Inject constructor(
    private val prefs: PreferencesService,
) {
    suspend fun recordScreenVisit(screenName: String) {
        val key = "visit_count_$screenName"
        prefs.putInt(key, prefs.getInt(key, 0) + 1)
    }
    
    suspend fun recordFeatureUsage(feature: String) {
        prefs.putBoolean("feature_used_$feature", true)
    }
    
    suspend fun getExperienceLevel(): ExperienceLevel {
        val totalVisits = prefs.getLaunchCount()
        val tutorialCompleted = prefs.isTutorialCompleted.first()
        
        var advancedFeaturesUsed = 0
        for (feature in ADVANCED_FEATURES) {
            if (prefs.getBoolean("feature_used_$feature", false)) {
                advancedFeaturesUsed++
            }
        }
        
        val forceShow = prefs.getBoolean("force_show_all_features", false)
        
        return when {
            forceShow -> ExperienceLevel.POWER_USER
            !tutorialCompleted -> ExperienceLevel.NEW
            totalVisits < 5 -> ExperienceLevel.LEARNING
            advancedFeaturesUsed < 3 -> ExperienceLevel.CASUAL
            else -> ExperienceLevel.POWER_USER
        }
    }
    
    suspend fun incrementAppOpens() {
        prefs.incrementLaunchCount()
    }
    
    companion object {
        private val ADVANCED_FEATURES = listOf(
            "backtester", "position_sizer", "trade_planner",
            "mtf_analyzer", "psychology_lock", "voice_commands",
            "content_studio", "deep_quant_analysis",
        )
    }
}

enum class ExperienceLevel { NEW, LEARNING, CASUAL, POWER_USER }
