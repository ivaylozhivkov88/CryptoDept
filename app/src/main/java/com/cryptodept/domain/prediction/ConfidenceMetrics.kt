package com.cryptodept.domain.prediction

/**
 * Comprehensive confidence metadata for a prediction.
 * Used for transparent display of AI/statistical model uncertainty.
 */
data class ConfidenceMetrics(
    val overallConfidence: Float,        // 0.0 - 1.0
    val modelAgreement: ModelAgreement,
    val dataQuality: DataQuality,
    val volatilityWarning: VolatilityLevel,
    val invalidationLevel: Double?,
    val historicalAccuracy: HistoricalAccuracy?,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ModelAgreement(
    val displayName: String,
    val emoji: String,
    val threshold: Float,
) {
    STRONG("Strong Consensus", "🟢", 0.75f),
    MODERATE("Moderate Agreement", "🟡", 0.50f),
    WEAK("Mixed Signals", "🟠", 0.30f),
    NONE("No Consensus", "🔴", 0.0f);
    
    companion object {
        fun fromRatio(agreeingModels: Int, totalModels: Int): ModelAgreement {
            if (totalModels == 0) return NONE
            val ratio = agreeingModels.toFloat() / totalModels
            return when {
                ratio >= STRONG.threshold -> STRONG
                ratio >= MODERATE.threshold -> MODERATE
                ratio >= WEAK.threshold -> WEAK
                else -> NONE
            }
        }
    }
}

enum class DataQuality(val displayName: String, val maxAgeMs: Long) {
    HIGH("Fresh (<5min)", 5 * 60 * 1000L),
    MEDIUM("Stale (5-30min)", 30 * 60 * 1000L),
    LOW("Outdated (>30min)", Long.MAX_VALUE),
    INSUFFICIENT("Not enough data", 0L);
    
    companion object {
        fun fromAge(lastUpdateMs: Long, dataPointCount: Int): DataQuality {
            if (dataPointCount < 20) return INSUFFICIENT
            val age = System.currentTimeMillis() - lastUpdateMs
            return when {
                age <= HIGH.maxAgeMs -> HIGH
                age <= MEDIUM.maxAgeMs -> MEDIUM
                else -> LOW
            }
        }
    }
}

enum class VolatilityLevel(val displayName: String, val warning: String?) {
    LOW("Calm market", null),
    NORMAL("Normal volatility", null),
    HIGH("High volatility", "Wide range expected"),
    EXTREME("Extreme volatility", "Predictions less reliable in current conditions");
    
    companion object {
        fun fromVolatility(volatility24h: Double): VolatilityLevel = when {
            volatility24h < 0.02 -> LOW
            volatility24h < 0.05 -> NORMAL
            volatility24h < 0.10 -> HIGH
            else -> EXTREME
        }
    }
}

data class HistoricalAccuracy(
    val sampleSize: Int,
    val accuracyPercent: Float,
    val timeframeText: String,
    val modelName: String? = null
) {
    val isReliable: Boolean get() = sampleSize >= 10
}
