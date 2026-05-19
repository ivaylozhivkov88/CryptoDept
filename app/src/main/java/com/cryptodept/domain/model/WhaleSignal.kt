package com.cryptodept.domain.model

enum class WhaleSignal(
    val emoji: String,
    val label: String,
    val description: String,
) {
    BULLISH_HEAVY(
        emoji = "🟢🟢",
        label = "Strong Buy Pressure",
        description = "Whales withdrawing significantly more than depositing.",
    ),
    BULLISH(
        emoji = "🟢",
        label = "Buy Pressure",
        description = "More whale withdrawals than deposits in last 24h.",
    ),
    NEUTRAL(
        emoji = "⚪",
        label = "Balanced",
        description = "Whale flow is balanced.",
    ),
    BEARISH(
        emoji = "🔴",
        label = "Sell Pressure",
        description = "More whale deposits than withdrawals in last 24h.",
    ),
    BEARISH_HEAVY(
        emoji = "🔴🔴",
        label = "Strong Sell Pressure",
        description = "Whales depositing significantly more than withdrawing.",
    ),
}
