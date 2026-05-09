package com.cryptodept.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_signal_rules")
data class CustomSignalRuleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val conditionsJson: String, // Stored as JSON
    val operator: String,
    val action: String,
    val isActive: Boolean,
)
