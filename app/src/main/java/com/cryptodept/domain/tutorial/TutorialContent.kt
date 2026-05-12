package com.cryptodept.domain.tutorial

import com.cryptodept.R

object TutorialContent {

    val ALL_CHAPTERS: List<TourChapter> = listOf(

        // ============================================
        // CHAPTER 1: DASHBOARD (8 steps)
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
                    target = TutorialTargetId.DASH_WHALE_FEED,
                    titleKey = R.string.tutorial_dash_4_title,
                    messageKey = R.string.tutorial_dash_4_message,
                    screenRoute = "dashboard"
                ),
                TourStep(
                    id = "dash-5",
                    target = TutorialTargetId.DASH_SENTIMENT_GAUGE,
                    titleKey = R.string.tutorial_dash_5_title,
                    messageKey = R.string.tutorial_dash_5_message,
                    screenRoute = "dashboard"
                ),
                TourStep(
                    id = "dash-6",
                    target = TutorialTargetId.DASH_NETWORK_HEALTH,
                    titleKey = R.string.tutorial_dash_6_title,
                    messageKey = R.string.tutorial_dash_6_message,
                    screenRoute = "dashboard"
                ),
                TourStep(
                    id = "dash-7",
                    target = TutorialTargetId.DASH_NAV_DRAWER,
                    titleKey = R.string.tutorial_dash_7_title,
                    messageKey = R.string.tutorial_dash_7_message,
                    screenRoute = "dashboard"
                ),
                TourStep(
                    id = "dash-8",
                    target = TutorialTargetId.DASH_QUICK_ACTIONS,
                    titleKey = R.string.tutorial_dash_8_title,
                    messageKey = R.string.tutorial_dash_8_message,
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
        // CHAPTER 3: ANALYSIS (5 steps)
        // ============================================
        TourChapter(
            screenRoute = "analysis",
            chapterTitleKey = R.string.tutorial_chapter_analysis,
            steps = listOf(
                TourStep("ana-1", TutorialTargetId.ANALYSIS_COIN_SELECTOR,
                    R.string.tutorial_ana_1_title, R.string.tutorial_ana_1_message, "analysis"),
                TourStep("ana-2", TutorialTargetId.ANALYSIS_INDICATORS,
                    R.string.tutorial_ana_2_title, R.string.tutorial_ana_2_message, "analysis"),
                TourStep("ana-3", TutorialTargetId.ANALYSIS_AI_VERDICT,
                    R.string.tutorial_ana_3_title, R.string.tutorial_ana_3_message, "analysis"),
                TourStep("ana-4", TutorialTargetId.ANALYSIS_PREDICTION,
                    R.string.tutorial_ana_4_title, R.string.tutorial_ana_4_message, "analysis"),
                TourStep("ana-5", TutorialTargetId.ANALYSIS_DEEP_SCAN,
                    R.string.tutorial_ana_5_title, R.string.tutorial_ana_5_message, "analysis")
            )
        ),

        // ============================================
        // CHAPTER 4: TOOLS HUB (8 steps - всеки tool отделно)
        // ============================================
        TourChapter(
            screenRoute = "tools_hub",
            chapterTitleKey = R.string.tutorial_chapter_tools,
            steps = listOf(
                TourStep("tools-1", TutorialTargetId.TOOLS_POSITION_SIZER,
                    R.string.tutorial_tools_1_title, R.string.tutorial_tools_1_message, "tools_hub"),
                TourStep("tools-2", TutorialTargetId.TOOLS_TRADE_PLANNER,
                    R.string.tutorial_tools_2_title, R.string.tutorial_tools_2_message, "tools_hub"),
                TourStep("tools-3", TutorialTargetId.TOOLS_MTF_ANALYZER,
                    R.string.tutorial_tools_3_title, R.string.tutorial_tools_3_message, "tools_hub"),
                TourStep("tools-4", TutorialTargetId.TOOLS_BACKTESTER,
                    R.string.tutorial_tools_4_title, R.string.tutorial_tools_4_message, "tools_hub"),
                TourStep("tools-5", TutorialTargetId.TOOLS_PSYCHOLOGY,
                    R.string.tutorial_tools_5_title, R.string.tutorial_tools_5_message, "tools_hub"),
                TourStep("tools-6", TutorialTargetId.TOOLS_WHALE_TRACKER,
                    R.string.tutorial_tools_6_title, R.string.tutorial_tools_6_message, "tools_hub"),
                TourStep("tools-7", TutorialTargetId.TOOLS_ENTRY_ANALYZER,
                    R.string.tutorial_tools_7_title, R.string.tutorial_tools_7_message, "tools_hub"),
                TourStep("tools-8", TutorialTargetId.TOOLS_CORRELATION,
                    R.string.tutorial_tools_8_title, R.string.tutorial_tools_8_message, "tools_hub")
            )
        ),

        // ============================================
        // CHAPTER 5: SIGNALS (3 steps)
        // ============================================
        TourChapter(
            screenRoute = "signals",
            chapterTitleKey = R.string.tutorial_chapter_signals,
            steps = listOf(
                TourStep("sig-1", TutorialTargetId.SIGNALS_LIST,
                    R.string.tutorial_sig_1_title, R.string.tutorial_sig_1_message, "signals"),
                TourStep("sig-2", TutorialTargetId.SIGNALS_COMPOSER,
                    R.string.tutorial_sig_2_title, R.string.tutorial_sig_2_message, "signals"),
                TourStep("sig-3", TutorialTargetId.SIGNALS_PERFORMANCE,
                    R.string.tutorial_sig_3_title, R.string.tutorial_sig_3_message, "signals")
            )
        ),

        // ============================================
        // CHAPTER 6: ALERTS (3 steps)
        // ============================================
        TourChapter(
            screenRoute = "alerts",
            chapterTitleKey = R.string.tutorial_chapter_alerts,
            steps = listOf(
                TourStep("alt-1", TutorialTargetId.ALERTS_LIST,
                    R.string.tutorial_alt_1_title, R.string.tutorial_alt_1_message, "alerts"),
                TourStep("alt-2", TutorialTargetId.ALERTS_COMPOSITE_BUILDER,
                    R.string.tutorial_alt_2_title, R.string.tutorial_alt_2_message, "alerts"),
                TourStep("alt-3", TutorialTargetId.ALERTS_PRIORITY,
                    R.string.tutorial_alt_3_title, R.string.tutorial_alt_3_message, "alerts")
            )
        ),

        // ============================================
        // CHAPTER 7: SETTINGS (2 steps)
        // ============================================
        TourChapter(
            screenRoute = "settings",
            chapterTitleKey = R.string.tutorial_chapter_settings,
            steps = listOf(
                TourStep("set-1", TutorialTargetId.SETTINGS_THEME,
                    R.string.tutorial_set_1_title, R.string.tutorial_set_1_message, "settings"),
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
     * Total number of steps — used for progress display "5/33".
     */
    val TOTAL_STEPS: Int = ALL_STEPS_FLAT.size
}
