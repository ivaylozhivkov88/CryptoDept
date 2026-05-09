package com.cryptodept.domain.usecase

import javax.inject.Inject

/**
 * Perform a professional deep-dive comparison between two crypto assets in a deep-dive format.
 */
class CoinComparisonPromptBuilder
    @Inject
    constructor() {
        fun build(
            coinA: String,
            coinB: String,
            yearTarget: String = "2026",
        ): String =
            """
            Perform a professional deep-dive comparison between $coinA and $coinB with a focus on their outlook for $yearTarget.
            
            REQUIRED CONTENT (in English):
            1. TABULAR COMPARISON: Create a markdown table comparing:
               - Technical Specs (Speed, Security, Scalability)
               - Ecosystem Health (TVL, Developers, Top Projects)
               - Risk Profile (Centralization, Regulation, Tech debt)
               - Main Narrative for $yearTarget
            2. PROS & CONS: Detailed list of advantages and disadvantages for each asset.
            3. THE "FLIPPENING" POTENTIAL: Discuss if one could overtake the other in market cap or adoption.
            4. FINAL VERDICT: A structured conclusion on which asset might be a better pick for $yearTarget based on current data. 
               - CRITICAL: Include a clear disclaimer that this is NOT financial advice.
            
            LANGUAGE: English (EN).
            TONE: Analytical, technical, objective. No hype.
            """.trimIndent()
    }
