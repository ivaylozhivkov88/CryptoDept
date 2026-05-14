package com.cryptodept.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.runner.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkerSmokeTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        val config =
            Configuration
                .Builder()
                .setExecutor(SynchronousExecutor())
                .build()

        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    @Test
    fun testAlertWorkerCanBeScheduled() {
        try {
            CryptoDataSyncWorker.schedule(context)
            assertTrue(true)
        } catch (e: Exception) {
            throw AssertionError("CryptoDataSyncWorker.schedule() should not throw: ${e.message}", e)
        }
    }

    @Test
    fun testBriefingWorkerScheduling() {
        try {
            DailyBriefingWorker.schedule(context)
            assertTrue(true)
        } catch (e: Exception) {
            throw AssertionError("DailyBriefingWorker.schedule() should not throw: ${e.message}", e)
        }
    }

    @Test
    fun testNewsWorkerScheduling() {
        try {
            NewsSyncWorker.schedule(context)
            assertTrue(true)
        } catch (e: Exception) {
            throw AssertionError("NewsSyncWorker.schedule() should not throw: ${e.message}", e)
        }
    }

    @Test
    fun testAgentWatchdogWorkerScheduling() {
        try {
            AgentWatchdogWorker.schedule(context)
            assertTrue(true)
        } catch (e: Exception) {
            throw AssertionError("AgentWatchdogWorker.schedule() should not throw: ${e.message}", e)
        }
    }

    @Test
    fun testAccuracyVerificationWorkerScheduling() {
        try {
            AccuracyVerificationWorker.schedule(context)
            assertTrue(true)
        } catch (e: Exception) {
            throw AssertionError("AccuracyVerificationWorker.schedule() should not throw: ${e.message}", e)
        }
    }

    @Test
    fun testNotificationChannelsExist() {
        try {
            val alertsId = com.cryptodept.util.NotificationChannels.ALERTS_CHANNEL_ID
            val briefingId = com.cryptodept.util.NotificationChannels.BRIEFING_CHANNEL_ID
            val liveId = com.cryptodept.util.NotificationChannels.LIVE_CHANNEL_ID

            assertTrue(alertsId.isNotEmpty())
            assertTrue(briefingId.isNotEmpty())
            assertTrue(liveId.isNotEmpty())
            assertTrue(setOf(alertsId, briefingId, liveId).size == 3)
        } catch (e: Exception) {
            throw AssertionError("NotificationChannels should be properly defined: ${e.message}", e)
        }
    }
}
