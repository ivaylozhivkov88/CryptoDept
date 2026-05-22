package com.cryptodept.domain.usecase

import com.cryptodept.data.db.TradeJournalDao
import com.cryptodept.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PsychologyAnalyzer
    @Inject
    constructor(
        private val journalDao: TradeJournalDao,
        private val settings: com.cryptodept.data.datastore.SystemSettingsManager,
    ) {
        suspend fun analyzeSession(): SessionStats =
            withContext(Dispatchers.Default) {
                // ... (rest of the code same)
                val today = System.currentTimeMillis() - 24 * 60 * 60 * 1000
                val allTrades = journalDao.getAllTradesSince(today)
                val closedTrades = allTrades.filter { it.status != "OPEN" }

                val wins = closedTrades.count { it.status == "CLOSED_WIN" }
                val losses = closedTrades.count { it.status == "CLOSED_LOSS" }

                // Consecutive losses/wins
                val sortedByTime = closedTrades.sortedByDescending { it.exitTime ?: 0 }
                var consecutiveLosses = 0
                var consecutiveWins = 0

                for (trade in sortedByTime) {
                    if (trade.status == "CLOSED_LOSS") {
                        consecutiveLosses++
                    } else {
                        break
                    }
                }
                for (trade in sortedByTime) {
                    if (trade.status == "CLOSED_WIN") {
                        consecutiveWins++
                    } else {
                        break
                    }
                }

                // Avg time between trades (minutes)
                val avgTime =
                    if (allTrades.size >= 2) {
                        val times =
                            allTrades
                                .mapNotNull { it.entryTime }
                                .sortedDescending()
                                .zipWithNext { a, b -> (a - b) / 60000 }
                        if (times.isNotEmpty()) times.average().toLong() else 60L
                    } else {
                        60L
                    }

                // Last trade size vs average
                val recentSizes = journalDao.getRecentSizes().filterNotNull()
                val avgSize = if (recentSizes.isNotEmpty()) recentSizes.average() else 0.0
                val lastSize = allTrades.maxByOrNull { it.entryTime }?.positionSizeUsd ?: 0.0
                val sizeRatio = if (avgSize > 0) lastSize / avgSize else 1.0

                val dayPnL = closedTrades.sumOf { it.pnlUsd ?: 0.0 }

                // Psychology Alerts
                val alerts = mutableListOf<PsychologyAlert>()

                if (consecutiveLosses >= 3) {
                    alerts.add(
                        PsychologyAlert(
                            PsychologyAlertType.REVENGE_TRADING,
                            AlertSeverity.CRITICAL,
                            "3+ CONSECUTIVE LOSSES DETECTED",
                            "Your win rate typically drops after 3+ consecutive losses.",
                            "STOP TRADING for at least 2 hours. Review your strategy.",
                        ),
                    )
                }

                if (sizeRatio > 1.8) {
                    alerts.add(
                        PsychologyAlert(
                            PsychologyAlertType.OVERSIZING,
                            AlertSeverity.WARNING,
                            "POSITION OVERSIZING DETECTED",
                            "Last trade was ${String.format(java.util.Locale.US, "%.1f", sizeRatio)}x your average size.",
                            "Emotional trading increases losses. Return to standard size.",
                        ),
                    )
                }

                if (closedTrades.size > 8) {
                    alerts.add(
                        PsychologyAlert(
                            PsychologyAlertType.OVERTRADING,
                            AlertSeverity.WARNING,
                            "OVERTRADING DETECTED",
                            "${closedTrades.size} trades today (your avg: 3-4).",
                            "More trades ≠ more profit. Quality over quantity.",
                        ),
                    )
                }

                if (avgTime < 10 && closedTrades.size > 3) {
                    alerts.add(
                        PsychologyAlert(
                            PsychologyAlertType.EMOTIONAL_TRADING,
                            AlertSeverity.WARNING,
                            "RAPID TRADING PATTERN",
                            "Avg $avgTime min between trades (healthy: 30+ min).",
                            "Slow down. Impulsive trades are rarely profitable.",
                        ),
                    )
                }

                if (consecutiveWins >= 5) {
                    alerts.add(
                        PsychologyAlert(
                            PsychologyAlertType.WINNING_STREAK_RISK,
                            AlertSeverity.INFO,
                            "5-TRADE WIN STREAK",
                            "Overconfidence after win streaks causes oversizing.",
                            "Maintain discipline. Keep standard position sizes.",
                        ),
                    )
                }

                val tiltScore =
                    minOf(
                        100,
                        (consecutiveLosses * 20) +
                            (if (sizeRatio > 1.5) 30 else 0) +
                            (if (avgTime < 15) 20 else 0) +
                            (if (closedTrades.size > 8) 15 else 0),
                    )

                val isTilt = tiltScore >= 60

                SessionStats(
                    tradesToday = allTrades.size,
                    winsToday = wins,
                    lossesToday = losses,
                    consecutiveLosses = consecutiveLosses,
                    consecutiveWins = consecutiveWins,
                    avgTimeBetweenTrades = avgTime,
                    lastTradeSizeVsAvg = sizeRatio,
                    dayPnL = dayPnL,
                    alerts = alerts,
                    isTiltDetected = isTilt,
                    tiltScore = tiltScore,
                )
            }
    }
