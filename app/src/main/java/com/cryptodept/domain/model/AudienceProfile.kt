package com.cryptodept.domain.model

enum class AudienceProfile(
    val label: String,
    val tone: String,
    val technicalLevel: String,
    val preferredLength: String,
) {
    DAY_TRADER(
        label = "Day Trader",
        tone = "Professional, fast-paced, data-heavy",
        technicalLevel = "High (Technical indicators, order flow, liquidations)",
        preferredLength = "Short to medium (Bullet points, quick stats)",
    ),
    HODLER(
        label = "Long-term Holder (HODLER)",
        tone = "Calm, macro-focused, narrative-driven",
        technicalLevel = "Medium (On-chain trends, macro events)",
        preferredLength = "Medium (Deep dives, long-term outlook)",
    ),
    DEFI_USER(
        label = "DeFi Degen / Yield Hunter",
        tone = "Exploratory, risk-aware, technical",
        technicalLevel = "High (TVL, APR/APY, smart contract risk, chains)",
        preferredLength = "Medium (Detailed protocol analysis)",
    ),
    CRYPTO_NEWBIE(
        label = "Crypto Newbie",
        tone = "Educational, simplified, cautious",
        technicalLevel = "Low (Concepts explained, no jargon)",
        preferredLength = "Short (Easy to digest steps)",
    ),
    CONTENT_FAN(
        label = "Content Consumer / Speculator",
        tone = "Engaging, dramatic, entertainment-focused",
        technicalLevel = "Low to Medium (Viral trends, price action)",
        preferredLength = "Variable (Hooks, short-form scripts)",
    ),
}
