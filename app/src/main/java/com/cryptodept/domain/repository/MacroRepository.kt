package com.cryptodept.domain.repository

import com.cryptodept.domain.model.CalendarEvent
import com.cryptodept.domain.model.MacroData

interface MacroRepository {
    suspend fun getMacroData(): Result<MacroData>
    suspend fun getCalendarEvents(): Result<List<CalendarEvent>>
}