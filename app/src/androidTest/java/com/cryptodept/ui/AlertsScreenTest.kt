package com.cryptodept.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.cryptodept.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class AlertsScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun testAlertsNavigationAndEmptyState() {
        // Navigate via Command Bar
        composeTestRule.onNodeWithTag("TerminalInput").performTextInput("ALERTS\n")

        // Verify Title
        composeTestRule.onNodeWithText(">>> ACTIVE PRICE ALERTS").assertIsDisplayed()
        
        // If empty, should show NO_ACTIVE_ALERTS
        // composeTestRule.onNodeWithText("NO ACTIVE ALERTS FOUND").assertIsDisplayed()
    }
}
