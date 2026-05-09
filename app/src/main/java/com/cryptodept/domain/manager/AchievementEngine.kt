package com.cryptodept.domain.manager

import com.cryptodept.data.datastore.PreferencesService
import com.cryptodept.domain.model.Achievement
import com.cryptodept.domain.model.AchievementCondition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementEngine
    @Inject
    constructor(
        private val preferencesService: PreferencesService,
    ) {
        private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
        val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()

        init {
            _achievements.value =
                listOf(
                    Achievement("first_alert", "FIRST SIGNAL", "Set your first price alert.", "🔔", null, AchievementCondition.FIRST_ALERT),
                    Achievement("predict_10", "PROPHET", "Make 10 price predictions.", "🔮", null, AchievementCondition.PREDICTIONS_COUNT),
                    Achievement(
                        "whale_watch",
                        "WHALE WATCHER",
                        "Track a transaction over $10M.",
                        "🐋",
                        null,
                        AchievementCondition.WHALE_TRACKED,
                    ),
                    Achievement("admin", "BIG BOSS", "Enter admin mode for the first time.", "🕴️", null, AchievementCondition.ADMIN_MODE),
                )
        }

        fun triggerCondition(
            condition: AchievementCondition,
            value: Int = 1,
        ) {
            val current = _achievements.value.toMutableList()
            val index = current.indexOfFirst { it.conditionType == condition }
            if (index != -1 && current[index].unlockedAt == null) {
                val shouldUnlock =
                    when (condition) {
                        AchievementCondition.PREDICTIONS_COUNT -> value >= 10
                        else -> true
                    }

                if (shouldUnlock) {
                    current[index] = current[index].copy(unlockedAt = System.currentTimeMillis())
                    _achievements.value = current
                }
            }
        }
    }
