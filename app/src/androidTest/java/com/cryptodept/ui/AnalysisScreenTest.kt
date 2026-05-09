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
class AnalysisScreenTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun testAnalysisNavigation() {
        // Navigate via Command Bar
        composeTestRule.onNodeWithTag("TerminalInput").performTextInput("ANALYSIS BTC\n")

        // Wait for content or title
        composeTestRule.onNodeWithText(">>> TERMINAL_DEPT_V3").assertIsDisplayed()

        // Check if Signal box is visible (contains strength text eventually)
        // We can't guarantee market data in UI tests without mocks, but we check title
        composeTestRule.onNodeWithText("BTC").assertExists()
    }

    @Test
    fun testDeepScanAction() {
        composeTestRule.onNodeWithTag("TerminalInput").performTextInput("ANALYSIS ETH\n")

        // Find and click Deep Scan button
        // Scroll if needed (AnalysisScreen is verticalScroll)
        composeTestRule.onNodeWithText("> RUN_DEEP_QUANT_SCAN").performScrollTo().performClick()

        // Verify if Paywall or Analysis loading screen appears (since it's a Pro feature)
        // If not Pro, should show Paywall
        composeTestRule.onNodeWithText(">>> CRYPTODEPT PRO REQUIRED").assertExists()
    }
}
