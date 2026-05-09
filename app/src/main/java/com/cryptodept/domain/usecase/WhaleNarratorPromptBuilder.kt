package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.WhaleTransaction
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject

/**
 * Generates dramatic TikTok/Reels scripts in English for whale transaction alerts.
 */
class WhaleNarratorPromptBuilder
    @Inject
    constructor() {
        fun build(tx: WhaleTransaction): String {
            val formatter = NumberFormat.getCurrencyInstance(Locale.US)
            val amountUsdStr = formatter.format(tx.amountUsd)

            return """
                Generate a high-energy 30-second TikTok script in English for a "Whale Alert".
                
                TRANSACTION DATA:
                - Symbol: ${tx.symbol}
                - Value: $amountUsdStr
                - Blockchain: ${tx.blockchain}
                - Source: ${if (tx.isExchange) "Exchange (${tx.exchangeName})" else "Private Wallet"}
                - Destination: ${tx.toAddress.take(6)}...${tx.toAddress.takeLast(4)}
                
                SCRIPT STRUCTURE (Total 30s):
                1. DRAMATIC OPENING (5s): A strong hook that stops the scroll. High stakes.
                2. TRANSACTION DETAIL (10s): State the facts clearly but dramatically. Use terms like "Whale" and "massive movement".
                3. MARKET IMPACT (10s): Speculate on the immediate price impact. Is it bullish or bearish?
                4. SUSPENSE ENDING (5s): A question or a teaser to check the next update.
                
                LANGUAGE: English (EN).
                TONE: Urgent, dramatic, "Breaking News" style.
                STRICT: No clichés like "to the moon". Focus on the data.
                """.trimIndent()
        }
    }
