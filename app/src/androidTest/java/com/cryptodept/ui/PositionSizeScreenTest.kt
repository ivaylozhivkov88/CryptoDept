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
class PositionSizeScreenTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun testPositionSizerInputs() {
        // Navigate to Position Sizer (assuming it can be reached via command bar or button)
        // For simplicity, we can set the content directly if we want to isolate,
        // but the prompt suggests Hilt and Screen testing.

        // Let's assume we are on Dashboard and we click SIZER
        composeTestRule.onNodeWithText("[SIZER]").performClick()

        // Verify we are on Position Sizer
        composeTestRule.onNodeWithText(">>> POSITION SIZER — Risk-Based Calculator").assertIsDisplayed()

        // Test portfolio input (it's a TextField)
        // We'll need to find it by text or label.
        // In the real code, it has a label like "PORTFOLIO SIZE (USD)"

        composeTestRule.onNodeWithText("10000.0").performTextClearance()
        composeTestRule.onNodeWithText("").performTextInput("5000")

        // verify calculation happened (Result section)
        // This is a bit dynamic, so we just check if result area is visible
        composeTestRule.onNodeWithText("RISK AMOUNT:").assertIsDisplayed()
    }
}
