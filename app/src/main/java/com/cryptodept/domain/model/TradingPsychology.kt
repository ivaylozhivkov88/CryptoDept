package com.cryptodept.domain.model

data class PsychologyAlert(
    val type: PsychologyAlertType,
    val severity: AlertSeverity,
    val title: String,
    val detail: String,
    val recommendation: String,
)

enum class PsychologyAlertType {
    REVENGE_TRADING, // Бързи trades след загуба
    OVERSIZING, // Позицията е по-голяма от нормалното
    OVERTRADING, // Твърде много trades за деня
    TILT, // Комбинация от горните
    WINNING_STREAK_RISK, // Прекалено уверен след поредица победи
    WORST_TRADING_HOURS, // Пот пазарни часове за потребителя
    EMOTIONAL_TRADING, // Твърде малко време между trades
}

enum class AlertSeverity { INFO, WARNING, CRITICAL }

data class SessionStats(
    val tradesToday: Int,
    val winsToday: Int,
    val lossesToday: Int,
    val consecutiveLosses: Int,
    val consecutiveWins: Int,
    val avgTimeBetweenTrades: Long, // Минути
    val lastTradeSizeVsAvg: Double, // Ratio (1.0 = нормален)
    val dayPnL: Double,
    val alerts: List<PsychologyAlert>,
    val isTiltDetected: Boolean,
    val tiltScore: Int, // 0-100
)
