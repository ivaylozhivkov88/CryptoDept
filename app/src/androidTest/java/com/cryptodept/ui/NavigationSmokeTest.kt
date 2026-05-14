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
class NavigationSmokeTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun runFullNavigationSmokeTest() {
        // 1. Wait for boot and landing on Dashboard
        composeTestRule.waitUntil(10000) {
            composeTestRule.onAllNodesWithText("DASHBOARD", substring = true).fetchSemanticsNodes().isNotEmpty()
        }

        // 2. Test Quick Access Buttons (Pro Gates)
        val buttons = listOf("SIZER", "PLANNER", "PREDICT", "RISK", "DERIVS", "JOURNAL")
        
        buttons.forEach { label ->
            composeTestRule.onNodeWithText("[$label]").performClick()
            
            // Should show either the screen or the Paywall
            // Check for [X] button or common Paywall text
            val isPaywallVisible = composeTestRule.onAllNodesWithText("CRYPTODEPT PRO", substring = true).fetchSemanticsNodes().isNotEmpty()
            val isScreenLoaded = !isPaywallVisible
            
            if (isPaywallVisible) {
                composeTestRule.onNodeWithText("[X]").performClick()
            } else {
                // If screen loaded, go back
                composeTestRule.onNodeWithTag("TerminalInput").performTextInput("BACK\n")
            }
            
            composeTestRule.waitForIdle()
        }

        // 3. Test Command Bar
        composeTestRule.onNodeWithTag("TerminalInput").performTextInput("HELP\n")
        composeTestRule.onNodeWithText(">>> SYSTEM_COMMAND_INDEX", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("OK").performClick()

        // 4. Navigate to Markets
        composeTestRule.onNodeWithTag("TerminalInput").performTextInput("MARKETS\n")
        composeTestRule.waitForIdle()
        
        // 5. Test Settings
        composeTestRule.onNodeWithTag("TerminalInput").performTextInput("SETTINGS\n")
        composeTestRule.onNodeWithText(">>> SYSTEM_SETTINGS_V2", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("< RETURN_TO_CORE").performClick()
    }
}
