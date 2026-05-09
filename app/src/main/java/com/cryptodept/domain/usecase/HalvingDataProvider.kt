package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.CyclePhase
import com.cryptodept.domain.model.HalvingCycle
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HalvingDataProvider
    @Inject
    constructor() {
        private val halvingDates =
            listOf(
                createDate(2012, 10, 28), // Cycle 1
                createDate(2016, 6, 9), // Cycle 2
                createDate(2020, 4, 11), // Cycle 3
                createDate(2024, 3, 20), // Cycle 4 (Current)
            )

        private val AVG_CYCLE_DAYS = 1458L // Approx 4 years

        fun getCurrentCycleInfo(): HalvingCycle {
            val now = System.currentTimeMillis()
            val lastHalving = halvingDates.last()
            val daysSince = (now - lastHalving) / (1000 * 60 * 60 * 24)

            val progress = (daysSince.toFloat() / AVG_CYCLE_DAYS).coerceIn(0f, 1f)

            val phase =
                when {
                    progress < 0.25f -> CyclePhase.ACCUMULATION
                    progress < 0.50f -> CyclePhase.BULL_EARLY
                    progress < 0.75f -> CyclePhase.BULL_LATE
                    else -> CyclePhase.BEAR
                }

            return HalvingCycle(
                cycleNumber = 4,
                halvingDate = lastHalving,
                daysSinceHalving = daysSince,
                progressToNextHalving = progress,
                currentPhase = phase,
                estimatedNextHalving = lastHalving + (AVG_CYCLE_DAYS * 24 * 60 * 60 * 1000),
            )
        }

        private fun createDate(
            year: Int,
            month: Int,
            day: Int,
        ): Long =
            Calendar
                .getInstance(TimeZone.getTimeZone("UTC"))
                .apply {
                    set(year, month, day, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
    }
