package com.cryptodept.data.repository

import com.cryptodept.domain.model.CalendarEvent
import com.cryptodept.domain.model.MacroData
import com.cryptodept.domain.repository.MacroRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MacroRepositoryImpl @Inject constructor() : MacroRepository {

    override suspend fun getMacroData(): Result<MacroData> {
        return try {
            val mockData = MacroData(
                sp500Price = 5000.0,
                sp500Change = 0.5,
                goldPrice = 2000.0,
                goldChange = -0.2,
                dxyPrice = 104.0,
                dxyChange = 0.1,
                btcSp500Correlation = 0.8,
                btcGoldCorrelation = -0.3,
                timestamp = System.currentTimeMillis()
            )
            Result.success(mockData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCalendarEvents(): Result<List<CalendarEvent>> {
        return try {
            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
