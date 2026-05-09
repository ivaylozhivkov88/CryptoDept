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
class SettingsScreenTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun testSettingsToggles() {
        // Navigate via Command Bar
        composeTestRule.onNodeWithTag("TerminalInput").performTextInput("SETTINGS\n")

        // Verify Title
        composeTestRule.onNodeWithText(">>> SYSTEM_SETTINGS_V2").assertIsDisplayed()

        // Test Audio Toggle (find by text in the same row if possible, or just click row)
        composeTestRule.onNodeWithText("AUDIO_FEEDBACK").performClick()

        // Test Back Button
        composeTestRule.onNodeWithText("< RETURN_TO_CORE").performClick()

        // Should be back on Dashboard
        composeTestRule.onNodeWithText(">>> MARKET TERMINAL v3.0", substring = true).assertIsDisplayed()
    }
}
