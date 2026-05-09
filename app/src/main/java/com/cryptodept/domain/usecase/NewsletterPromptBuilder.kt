package com.cryptodept.domain.usecase

import javax.inject.Inject

/**
 * Generates prompts for long-form weekly newsletter content (Substack/MailerLite).
 */
class NewsletterPromptBuilder
    @Inject
    constructor() {
        fun build(
            topEvents: List<String>,
            bigMoverSymbol: String,
            nextWeekOutlook: String,
        ): String =
            """
            Write a comprehensive weekly crypto newsletter digest in English.
            Target platform: Substack / MailerLite.
            Approximate length: 600 words.
            
            REQUIRED STRUCTURE:
            1. SUBJECT LINES: Provide two variants (A: Curiosity-driven, B: Direct and data-driven).
            2. SECTION 1: "Top 3 Newsworthy Events" - In-depth analysis of these events: ${topEvents.joinToString(", ")}.
            3. SECTION 2: "Big Mover of the Week" - Focus on $bigMoverSymbol. Why it moved, volume analysis, and what's next.
            4. SECTION 3: "Watching Next Week" - Key economic dates, token unlocks, or expected announcements. Outlook: $nextWeekOutlook.
            
            LANGUAGE: English (EN).
            TONE: Professional, insightful, like a premium financial briefing.
            RULES: No hype, no price predictions without context. Use bullet points for readability.
            """.trimIndent()
    }
