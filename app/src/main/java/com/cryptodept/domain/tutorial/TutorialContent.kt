package com.cryptodept.domain.tutorial

import com.cryptodept.R

object TutorialContent {

    val ALL_CHAPTERS: List<TourChapter> = listOf(
        TourChapter(
            screenRoute = "dashboard",
            chapterTitleKey = R.string.tutorial_chapter_dashboard,
            steps = listOf(
                // Step 1: Welcome
                TourStep(
                    id = "welcome",
                    target = TutorialTargetId.DASH_PRICE_TICKER,
                    titleKey = R.string.tutorial_dash_1_title, // "WELCOME TO TERMINAL"
                    messageKey = R.string.tutorial_dash_1_message,
                    screenRoute = "dashboard"
                ),
                // Step 2: AI Narrative
                TourStep(
                    id = "ai-narrative",
                    target = TutorialTargetId.DASH_AI_NARRATIVE,
                    titleKey = R.string.tutorial_dash_2_title,
                    messageKey = R.string.tutorial_dash_2_message,
                    screenRoute = "dashboard"
                ),
                // Step 3: Sentiment
                TourStep(
                    id = "sentiment",
                    target = TutorialTargetId.DASH_SENTIMENT_GAUGE,
                    titleKey = R.string.tutorial_dash_5_title,
                    messageKey = R.string.tutorial_dash_5_message,
                    screenRoute = "dashboard"
                )
            )
        ),
        TourChapter(
            screenRoute = "alerts",
            chapterTitleKey = R.string.tutorial_chapter_alerts,
            steps = listOf(
                // Step 4: First Alert
                TourStep(
                    id = "first-alert",
                    target = TutorialTargetId.ALERTS_LIST,
                    titleKey = R.string.tutorial_merged_alerts_title,
                    messageKey = R.string.tutorial_merged_alerts_message,
                    screenRoute = "alerts"
                )
            )
        ),
        TourChapter(
            screenRoute = "settings",
            chapterTitleKey = R.string.tutorial_chapter_settings,
            steps = listOf(
                // Step 5: Ready
                TourStep(
                    id = "ready",
                    target = TutorialTargetId.SETTINGS_TIER,
                    titleKey = R.string.tutorial_set_tier_title,
                    messageKey = R.string.tutorial_set_tier_message,
                    screenRoute = "settings"
                )
            )
        )
    )

    val ALL_STEPS_FLAT: List<TourStep> = ALL_CHAPTERS.flatMap { it.steps }
    val TOTAL_STEPS: Int = ALL_STEPS_FLAT.size
}
