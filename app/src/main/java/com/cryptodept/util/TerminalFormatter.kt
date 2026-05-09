package com.cryptodept.util

import java.util.*

object TerminalFormatter {
    fun formatPrice(value: Double): String =
        if (value < 1.0) {
            String.format(Locale.US, "%.6f", value)
        } else if (value < 100) {
            String.format(Locale.US, "%.4f", value)
        } else {
            String.format(Locale.US, "%,.2f", value)
        }

    fun formatPercent(
        value: Double,
        includeSign: Boolean = false,
    ): String {
        val sign = if (includeSign && value > 0) "+" else ""
        return "$sign${String.format(Locale.US, "%.2f", value)}%"
    }

    fun formatCurrency(value: Double): String = "$${formatPrice(value)}"

    fun formatCompactNumber(n: Double): String =
        when {
            n >= 1_000_000_000_000 -> String.format(Locale.US, "%.2fT", n / 1_000_000_000_000)
            n >= 1_000_000_000 -> String.format(Locale.US, "%.2fB", n / 1_000_000_000)
            n >= 1_000_000 -> String.format(Locale.US, "%.2fM", n / 1_000_000)
            else -> String.format(Locale.US, "%,.0f", n)
        }

    fun formatShortAddress(address: String): String {
        if (address.length <= 12) return address
        return "${address.take(6)}...${address.takeLast(4)}"
    }
}
