package com.cryptodept.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cryptodept.domain.model.CoinPrice
import com.cryptodept.domain.model.EventPriority
import com.cryptodept.domain.model.EventType
import com.cryptodept.domain.model.SystemEvent
import com.cryptodept.ui.alerts.SwipeToDeleteWrapper
import com.cryptodept.ui.components.*
import com.cryptodept.ui.dashboard.*
import com.cryptodept.ui.theme.CryptoDeptTheme
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test

class ComponentTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testTickerTapeDisplay() {
        val prices =
            listOf(
                CoinPrice(
                    id = "bitcoin",
                    symbol = "BTC",
                    name = "Bitcoin",
                    currentPrice = 65000.0,
                    priceChange24h = 500.0,
                    priceChangePercentage24h = 1.2,
                    marketCap = 1200000000.0,
                    totalVolume = 30000000.0,
                    high24h = 66000.0,
                    low24h = 64000.0,
                    lastUpdated = System.currentTimeMillis(),
                ),
            )

        composeTestRule.setContent {
            CryptoDeptTheme {
                TickerTape(prices = prices, networkHealth = null)
            }
        }

        composeTestRule.onNodeWithText("BTC $65000.00", substring = true).assertExists()
    }

    @Test
    fun testQuickAccessButtonClick() {
        val navController = mockk<NavController>(relaxed = true)

        composeTestRule.setContent {
            CryptoDeptTheme {
                QuickAccessButton(label = "TEST", route = "test_route", navController = navController)
            }
        }

        composeTestRule.onNodeWithText("[TEST]").performClick()

        verify { navController.navigate("test_route") }
    }

    @Test
    fun testSwipeToDelete() {
        var deleteCalled = false

        composeTestRule.setContent {
            CryptoDeptTheme {
                SwipeToDeleteWrapper(onDelete = { deleteCalled = true }) {
                    Box(modifier = Modifier.fillMaxWidth().height(50.dp).testTag("Item")) {
                        Text("Swipe Me")
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("Item").performTouchInput {
            swipeLeft()
        }

        composeTestRule.waitForIdle()
        assert(deleteCalled)
    }

    @Test
    fun testSentimentBadgeDisplay() {
        composeTestRule.setContent {
            CryptoDeptTheme {
                SentimentBadge(pulse = 85, label = "BULLISH")
            }
        }

        composeTestRule.onNodeWithText("85%").assertIsDisplayed()
        composeTestRule.onNodeWithText("BULLISH").assertIsDisplayed()
    }

    @Test
    fun testEventLogRowDisplay() {
        val event =
            SystemEvent(
                id = "1",
                message = "BTC BREAKOUT DETECTED",
                timestamp = System.currentTimeMillis(),
                type = EventType.MARKET_SIGNAL,
                priority = EventPriority.HIGH,
            )

        composeTestRule.setContent {
            CryptoDeptTheme {
                EventLogRow(event = event)
            }
        }

        composeTestRule.onNodeWithText("BTC BREAKOUT DETECTED").assertIsDisplayed()
    }

    @Test
    fun testTerminalHelpDialogDisplay() {
        composeTestRule.setContent {
            CryptoDeptTheme {
                TerminalHelpDialog(onDismiss = {})
            }
        }

        composeTestRule.onNodeWithText(">>> SYSTEM_COMMAND_INDEX", substring = true).assertIsDisplayed()
    }

    @Test
    fun testNetworkStatDisplay() {
        composeTestRule.setContent {
            CryptoDeptTheme {
                NetworkStat(label = "GAS", value = "25 Gwei")
            }
        }

        composeTestRule.onNodeWithText("GAS").assertIsDisplayed()
        composeTestRule.onNodeWithText("25 Gwei").assertIsDisplayed()
    }
}
