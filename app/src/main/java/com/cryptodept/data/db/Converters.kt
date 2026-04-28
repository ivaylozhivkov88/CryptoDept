package com.cryptodept.data.db

import androidx.room.TypeConverter
import com.cryptodept.domain.model.AlertDirection

class Converters {
    @TypeConverter
    fun fromAlertDirection(value: AlertDirection): String {
        return value.name
    }

    @TypeConverter
    fun toAlertDirection(value: String): AlertDirection {
        return AlertDirection.valueOf(value)
    }
}