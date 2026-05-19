package com.cryptodept.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.cryptodept.ui.onboarding.OnboardingScreen
import com.cryptodept.ui.theme.CryptoDeptTheme
import org.junit.Rule
import org.junit.Test

class OnboardingScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testOnboardingSequence() {
        var completed = false

        composeTestRule.setContent {
            CryptoDeptTheme {
                OnboardingScreen(onOnboardingComplete = { completed = true })
            }
        }

        // Slide 1: Boot Sequence (Click anywhere to skip typing animation)
        composeTestRule.onRoot().performClick()

        // Slide 2: Risk Disclaimer
        composeTestRule.onNodeWithText("I UNDERSTAND THE RISKS").performClick()
        composeTestRule.onNodeWithText("[ENTER THE TERMINAL →]").performClick()

        assert(completed)
    }
}
