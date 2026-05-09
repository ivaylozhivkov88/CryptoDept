package com.cryptodept.domain.model

data class EntryAnalysis(
    val coin: String,
    val currentPrice: Double,
    val entryScore: Int, // 0-100
    val verdict: EntryVerdict,
    val whyNotNow: List<String>, // Причини против влизане сега
    val betterZones: List<EntryZone>,
    val immediateAlertSuggested: Boolean,
)

data class EntryZone(
    val type: ZoneType, // IDEAL, DECENT, LAST_RESORT
    val priceFrom: Double,
    val priceTo: Double,
    val reason: String,
    val projectedRsi: Double?,
    val confluenceCount: Int,
)

enum class ZoneType(
    val label: String,
    val color: Long,
) {
    IDEAL("🟢 IDEAL ZONE", 0xFF00FF41),
    DECENT("🟡 DECENT ZONE", 0xFFFFB000),
    LAST_RESORT("🔴 LAST RESORT", 0xFFFF6600),
}

enum class EntryVerdict {
    EXCELLENT_NOW, // Score > 75 → влизай сега
    ACCEPTABLE_NOW, // Score 50-75 → може, но внимавай
    WAIT, // Score 30-50 → по-добре изчакай
    AVOID_NOW, // Score < 30 → лош момент
}
