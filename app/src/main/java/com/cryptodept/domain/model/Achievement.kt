package com.cryptodept.domain.model

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String, // Resource name or emoji
    val unlockedAt: Long? = null,
    val conditionType: AchievementCondition,
)

enum class AchievementCondition {
    FIRST_ALERT,
    PREDICTIONS_COUNT,
    STREAK_7DAYS,
    WHALE_TRACKED,
    ADMIN_MODE,
    FIRST_TRADE,
}
