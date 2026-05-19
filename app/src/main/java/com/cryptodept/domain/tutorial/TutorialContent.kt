package com.cryptodept.domain.tutorial

import com.cryptodept.R

object TutorialContent {

    val ALL_CHAPTERS: List<TourChapter> = listOf(

        // ============================================
        // CHAPTER 1: DASHBOARD (7 steps)
        // ============================================
        TourChapter(
            screenRoute = "dashboard",
            chapterTitleKey = R.string.tutorial_chapter_dashboard,
            steps = listOf(
                TourStep(
                    id = "dash-1",
                    target = TutorialTargetId.DASH_PRICE_TICKER,
                    titleKey = R.string.tutorial_dash_1_title,
                    messageKey = R.string.tutorial_dash_1_message,
                    screenRoute = "dashboard"
                ),
                TourStep(
                    id = "dash-2",
                    target = TutorialTargetId.DASH_AI_NARRATIVE,
                    titleKey = R.string.tutorial_dash_2_title,
                    messageKey = R.string.tutorial_dash_2_message,
                    screenRoute = "dashboard"
                ),
                TourStep(
                    id = "dash-3",
                    target = TutorialTargetId.DASH_TOP_MOVERS,
                    titleKey = R.string.tutorial_dash_3_title,
                    messageKey = R.string.tutorial_dash_3_message,
                    screenRoute = "dashboard"
                ),
                TourStep(
                    id = "dash-4",
                    target = TutorialTargetId.DASH_SENTIMENT_GAUGE,
                    titleKey = R.string.tutorial_dash_5_title,
                    messageKey = R.string.tutorial_dash_5_message,
                    screenRoute = "dashboard"
                ),
                TourStep(
                    id = "dash-5",
                    target = TutorialTargetId.DASH_NETWORK_HEALTH,
                    titleKey = R.string.tutorial_dash_6_title,
                    messageKey = R.string.tutorial_dash_6_message,
                    screenRoute = "dashboard"
                )
            )
        ),

        // ============================================
        // CHAPTER 2: MARKETS (4 steps)
        // ============================================
        TourChapter(
            screenRoute = "markets",
            chapterTitleKey = R.string.tutorial_chapter_markets,
            steps = listOf(
                TourStep("mkt-1", TutorialTargetId.MARKETS_GLOBAL_STATS,
                    R.string.tutorial_mkt_1_title, R.string.tutorial_mkt_1_message, "markets"),
                TourStep("mkt-2", TutorialTargetId.MARKETS_LIST,
                    R.string.tutorial_mkt_2_title, R.string.tutorial_mkt_2_message, "markets"),
                TourStep("mkt-3", TutorialTargetId.MARKETS_SORT_FILTER,
                    R.string.tutorial_mkt_3_title, R.string.tutorial_mkt_3_message, "markets"),
                TourStep("mkt-4", TutorialTargetId.MARKETS_FAVORITES,
                    R.string.tutorial_mkt_4_title, R.string.tutorial_mkt_4_message, "markets")
            )
        ),

        // ============================================
        // CHAPTER 3: ANALYSIS (Merged)
        // ============================================
        TourChapter(
            screenRoute = "analysis",
            chapterTitleKey = R.string.tutorial_chapter_analysis,
            steps = listOf(
                TourStep("ana-merged", TutorialTargetId.ANALYSIS_INDICATORS,
                    R.string.tutorial_merged_analysis_title, R.string.tutorial_merged_analysis_message, "analysis")
            )
        ),

        // ============================================
        // CHAPTER 4: TRADER TOOLS & SIGNALS (Merged)
        // ============================================
        TourChapter(
            screenRoute = "tools_hub",
            chapterTitleKey = R.string.tutorial_chapter_tools,
            steps = listOf(
                TourStep("tools-merged", TutorialTargetId.TOOLS_POSITION_SIZER,
                    R.string.tutorial_merged_tools_title, R.string.tutorial_merged_tools_message, "tools_hub")
            )
        ),

        // ============================================
        // CHAPTER 5: ALERTS (Merged)
        // ============================================
        TourChapter(
            screenRoute = "alerts",
            chapterTitleKey = R.string.tutorial_chapter_alerts,
            steps = listOf(
                TourStep("alt-merged", TutorialTargetId.ALERTS_LIST,
                    R.string.tutorial_merged_alerts_title, R.string.tutorial_merged_alerts_message, "alerts")
            )
        ),

        // ============================================
        // CHAPTER 6: SETTINGS (2 steps)
        // ============================================
        TourChapter(
            screenRoute = "settings",
            chapterTitleKey = R.string.tutorial_chapter_settings,
            steps = listOf(
                TourStep("set-1", TutorialTargetId.SETTINGS_THEME,
                    R.string.tutorial_set_1_title, R.string.tutorial_set_1_message, "settings"),
                TourStep("set-glossary", TutorialTargetId.SETTINGS_GLOSSARY,
                    R.string.tutorial_set_glossary_title, R.string.tutorial_set_glossary_message, "settings"),
                TourStep("set-tier", TutorialTargetId.SETTINGS_TIER,
                    R.string.tutorial_set_tier_title, R.string.tutorial_set_tier_message, "settings"),
                TourStep("set-2", TutorialTargetId.SETTINGS_REPLAY_TUTORIAL,
                    R.string.tutorial_set_2_title, R.string.tutorial_set_2_message, "settings")
            )
        )
    )

    /**
     * Flat list of all steps in order.
     */
    val ALL_STEPS_FLAT: List<TourStep> = ALL_CHAPTERS.flatMap { it.steps }

    /**
     * Total number of steps — used for progress display "5/31".
     */
    val TOTAL_STEPS: Int = ALL_STEPS_FLAT.size
}
