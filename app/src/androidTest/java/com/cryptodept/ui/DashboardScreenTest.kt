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
class DashboardScreenTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun testDashboardElements() {
        // Wait for Loading to finish (Skeleton -> Content)
        composeTestRule.onNodeWithText(">>> MARKET TERMINAL v3.0", substring = true).assertIsDisplayed()

        // Check TickerTape
        composeTestRule.onNodeWithTag("TickerTape").assertExists()

        // Check CommandBar
        composeTestRule.onNodeWithTag("TerminalInput").assertExists()
    }

    @Test
    fun testHelpCommand() {
        composeTestRule.onNodeWithTag("TerminalInput").performTextInput("HELP\n")
        composeTestRule.onNodeWithText("TERMINAL COMMANDS").assertIsDisplayed()
        composeTestRule.onNodeWithText("CLOSE").performClick()
        composeTestRule.onNodeWithText("TERMINAL COMMANDS").assertDoesNotExist()
    }
}
