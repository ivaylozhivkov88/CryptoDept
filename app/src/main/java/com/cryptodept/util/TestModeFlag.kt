package com.cryptodept.util

import com.cryptodept.BuildConfig

/**
 * Centralized flag for "test period" features.
 * 
 * Test period features:
 * - Test purchase button on paywall
 * - Bypass paywall in debug builds
 * 
 * After production launch (Day 28+):
 *   Set IS_TEST_PERIOD = false
 *   Test buttons disappear from all screens.
 *   Admin users (by email) still have full access.
 * 
 * DO NOT remove this flag — useful for future beta tests.
 */
object TestModeFlag {
    
    /**
     * Set to `false` after production launch (after May 27, 2026).
     * 
     * Currently: FALSE — production rules enforced.
     */
    const val IS_TEST_PERIOD: Boolean = false
    
    /**
     * Shows test purchase button on paywall when both conditions met:
     * - We're in test period
     * - This is debug build (never in release builds)
     */
    val SHOW_TEST_PURCHASE_BUTTON: Boolean
        get() = IS_TEST_PERIOD && BuildConfig.DEBUG
    
    /**
     * Bypass paywall checks in dev builds during test period.
     */
    val BYPASS_PAYWALL_IN_DEBUG: Boolean
        get() = IS_TEST_PERIOD && BuildConfig.DEBUG
}
