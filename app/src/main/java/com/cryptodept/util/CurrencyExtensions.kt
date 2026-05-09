package com.cryptodept.util

import java.util.Locale

/**
 * Extension functions for consistent currency and number formatting across the app.
 */

fun Double.toCurrency(decimals: Int = 2): String {
    val pattern = if (decimals == 0) "%,.0f" else "%,.${decimals}f"
    return "$${String.format(Locale.US, pattern, this)}"
}

fun Double.toPercentage(
    includePlus: Boolean = false,
    decimals: Int = 2,
): String {
    val pattern = if (includePlus && this > 0) "+%.${decimals}f%%" else "%.${decimals}f%%"
    return String.format(Locale.US, pattern, this)
}

fun Double.toCompactCurrency(): String =
    when {
        this >= 1_000_000_000_000 -> String.format(Locale.US, "$%.1fT", this / 1_000_000_000_000)
        this >= 1_000_000_000 -> String.format(Locale.US, "$%.1fB", this / 1_000_000_000)
        this >= 1_000_000 -> String.format(Locale.US, "$%.1fM", this / 1_000_000)
        else -> this.toCurrency(0)
    }
