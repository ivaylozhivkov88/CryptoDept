package com.cryptodept.util

import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

enum class MarketSession(val displayName: String, val briefKey: String) {
    MORNING("Pre-London / Morning Brief", "MORNING"),
    ACTIVE("London + NY Active", "ACTIVE"),
    NY_OPEN("NY Open — High Alert", "ACTIVE"),
    EVENING("Daily Review", "EVENING"),
    ASIAN("Asian Session", "MORNING"),
    OVERNIGHT("Overnight / Low Volume", "EVENING")
}

@Singleton
class MarketSessionManager @Inject constructor() {

    fun getCurrentSession(): MarketSession {
        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val utcHour = utcCalendar.get(Calendar.HOUR_OF_DAY)
        return when (utcHour) {
            in 5..7   -> MarketSession.MORNING   // Pre-London
            in 8..11  -> MarketSession.ACTIVE    // London Active
            in 12..15 -> MarketSession.NY_OPEN   // NY Open
            in 16..19 -> MarketSession.ACTIVE    // Late NY
            in 20..23 -> MarketSession.EVENING   // Daily Review
            else      -> MarketSession.OVERNIGHT // 00:00-04:00 UTC
        }
    }

    fun getSessionColor(session: MarketSession): Long = when (session) {
        MarketSession.MORNING   -> 0xFF00FF41 // phosphor green
        MarketSession.ACTIVE    -> 0xFFFFB800 // amber — caution
        MarketSession.NY_OPEN   -> 0xFFFF3B30 // red — high alert
        MarketSession.EVENING   -> 0xFF888888 // dim — review mode
        MarketSession.ASIAN     -> 0xFF00BFFF // blue — calm
        MarketSession.OVERNIGHT -> 0xFF444444 // very dim
    }
}
