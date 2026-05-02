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
class TradeJournalTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun testJournalOpenAndAddTrade() {
        // Navigate via Command Bar
        composeTestRule.onNodeWithTag("TerminalInput").performTextInput("JOURNAL\n")

        // Verify Title
        composeTestRule.onNodeWithText(">>> TRADE JOURNAL").assertIsDisplayed()

        // Click Add FAB
        composeTestRule.onNodeWithContentDescription("Add Trade").performClick()

        // Verify Add Sheet
        composeTestRule.onNodeWithText(">>> NEW TRADE").assertIsDisplayed()
        
        // Fill simple data
        composeTestRule.onAllNodesWithText("Symbol").onFirst().performTextInput("SOL")
        
        // Save
        composeTestRule.onNodeWithText("[SAVE TRADE]").performClick()
        
        // Check if trade list title is back
        composeTestRule.onNodeWithText(">>> TRADE JOURNAL").assertIsDisplayed()
    }
}
