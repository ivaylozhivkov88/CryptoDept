package com.cryptodept.domain.usecase

import javax.inject.Inject

/**
 * Generates prompts for speculative "What If" scenarios for weekly market analysis.
 */
class SpeculativeScenarioPromptBuilder
    @Inject
    constructor() {
        fun build(
            asset: String,
            targetPrice: String,
            timeframe: String = "next week",
        ): String =
            """
            Generate a speculative "What If" market scenario in English.
            
            CORE SCENARIO: "What if $asset drops to/reaches $targetPrice $timeframe?"
            
            CONTENT STRUCTURE:
            1. THE SETUP: Describe the hypothetical situation and why it's being discussed.
            2. ARGUMENTS FOR (Bullish/Bearish case): Provide 3 logical reasons why this could actually happen based on current market trends.
            3. ARGUMENTS AGAINST (The counter-case): Provide 3 strong reasons why this scenario might fail or be delayed.
            4. MARKET IMPACT: If this target is hit, what happens to the broader crypto ecosystem? (Liquidations, sentiment shift, etc.)
            5. ENGAGEMENT: End with a compelling question to the audience to encourage comments.
            
            LANGUAGE: English (EN).
            TONE: Objective, speculative but grounded in logic. Avoid sensationalism.
            RULES: No "to the moon" or "crash incoming" without backing it up with data.
            """.trimIndent()
    }
