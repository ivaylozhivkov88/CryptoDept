package com.cryptodept.data.repository

import com.cryptodept.data.db.AlertDao
import com.cryptodept.data.db.AlertEntity
import com.cryptodept.domain.model.AlertDirection
import com.cryptodept.service.AlertNotificationService
import com.cryptodept.util.AnalyticsService
import com.cryptodept.util.HapticService
import com.google.gson.Gson
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class AlertsRepositoryTest {
    private val alertDao: AlertDao = mockk(relaxed = true)
    private val alertNotificationService: AlertNotificationService = mockk(relaxed = true)
    private val hapticService: HapticService = mockk(relaxed = true)
    private val analytics: AnalyticsService = mockk(relaxed = true)
    private val gson: Gson = Gson()

    private lateinit var repository: AlertsRepositoryImpl

    @Before
    fun setup() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any<String>(), any<String>()) } returns 0
        every { android.util.Log.i(any<String>(), any<String>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>()) } returns 0

        repository = AlertsRepositoryImpl(
            alertDao,
            alertNotificationService,
            hapticService,
            analytics,
            gson
        )
    }

    @Test
    fun `checkAlerts triggers notification when price goes ABOVE target`() = runTest {
        val alert = AlertEntity(
            id = 1,
            coinId = "bitcoin",
            coinSymbol = "BTC",
            targetPrice = 60000.0,
            direction = AlertDirection.ABOVE,
            isActive = true,
            isTriggered = false,
            createdAt = System.currentTimeMillis()
        )
        
        coEvery { alertDao.getActiveAlerts() } returns listOf(alert)

        // Trigger: Current price 61000 is above 60000
        repository.checkAlerts("bitcoin", 61000.0)

        coVerify { alertDao.markAsTriggered(1) }
        coVerify { alertNotificationService.showPriceAlert(any(), 61000.0) }
        coVerify { hapticService.alert() }
    }

    @Test
    fun `checkAlerts does NOT trigger when price is BELOW target for ABOVE direction`() = runTest {
        val alert = AlertEntity(
            id = 1,
            coinId = "bitcoin",
            coinSymbol = "BTC",
            targetPrice = 60000.0,
            direction = AlertDirection.ABOVE,
            isActive = true,
            isTriggered = false,
            createdAt = System.currentTimeMillis()
        )
        
        coEvery { alertDao.getActiveAlerts() } returns listOf(alert)

        // No trigger: 59000 is not above 60000
        repository.checkAlerts("bitcoin", 59000.0)

        coVerify(exactly = 0) { alertDao.markAsTriggered(any()) }
        coVerify(exactly = 0) { alertNotificationService.showPriceAlert(any(), any()) }
    }
}
