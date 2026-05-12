package com.cryptodept.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "intelligence_briefings")
data class IntelligenceBriefingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val summary: String,
    val anomalyScore: Int,
    val sentiment: String,
    val riskScore: Int,
    val evidence: String // JSON or comma-separated links/evidence
)
