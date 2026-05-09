package com.cryptodept.domain.repository

import com.cryptodept.domain.model.CalendarEvent
import com.cryptodept.domain.model.MacroCorrelation
import com.cryptodept.domain.model.MacroData
import com.cryptodept.domain.model.MacroDataPoint

interface MacroRepository {
    suspend fun getMacroData(): Result<MacroData>

    suspend fun getCalendarEvents(): Result<List<CalendarEvent>>

    suspend fun getMacroCorrelations(): Result<List<MacroCorrelation>>

    suspend fun getAssetTimeSeries(symbol: String): Result<List<MacroDataPoint>>
}
