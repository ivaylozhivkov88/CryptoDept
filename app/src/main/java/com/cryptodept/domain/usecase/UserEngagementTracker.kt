package com.cryptodept.domain.usecase

import com.cryptodept.data.datastore.SystemSettingsManager
import com.cryptodept.data.datastore.UserSessionManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserEngagementTracker @Inject constructor(
    private val session: UserSessionManager,
    private val settings: SystemSettingsManager,
) {
    suspend fun recordScreenVisit(screenName: String) {
        val key = "visit_count_$screenName"
        session.putInt(key, session.getInt(key, 0) + 1)
    }
    
    suspend fun recordFeatureUsage(feature: String) {
        session.putBoolean("feature_used_$feature", true)
    }
    
    suspend fun getExperienceLevel(): ExperienceLevel {
        val totalVisits = session.getLaunchCount()
        val tutorialCompleted = session.isTutorialCompleted.first()
        
        var advancedFeaturesUsed = 0
        for (feature in ADVANCED_FEATURES) {
            if (session.getBoolean("feature_used_$feature", false)) {
                advancedFeaturesUsed++
            }
        }
        
        val forceShow = settings.forceShowAllFeatures.first()
        
        return when {
            forceShow -> ExperienceLevel.POWER_USER
            !tutorialCompleted -> ExperienceLevel.NEW
            totalVisits < 5 -> ExperienceLevel.LEARNING
            advancedFeaturesUsed < 3 -> ExperienceLevel.CASUAL
            else -> ExperienceLevel.POWER_USER
        }
    }
    
    suspend fun incrementAppOpens() {
        session.incrementLaunchCount()
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
