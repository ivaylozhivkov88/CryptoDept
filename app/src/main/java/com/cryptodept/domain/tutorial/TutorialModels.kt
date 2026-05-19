package com.cryptodept.domain.tutorial

/**
 * Unique identifier for every UI element that can be a tutorial target.
 * Add new entries here when adding new tutorial steps.
 */
enum class TutorialTargetId {
    // DASHBOARD
    DASH_PRICE_TICKER,
    DASH_AI_NARRATIVE,
    DASH_TOP_MOVERS,
    DASH_WHALE_FEED,
    DASH_SENTIMENT_GAUGE,
    DASH_NETWORK_HEALTH,
    DASH_NAV_DRAWER,
    DASH_QUICK_ACTIONS,

    // MARKETS
    MARKETS_LIST,
    MARKETS_SORT_FILTER,
    MARKETS_FAVORITES,
    MARKETS_GLOBAL_STATS,

    // ANALYSIS
    ANALYSIS_COIN_SELECTOR,
    ANALYSIS_INDICATORS,
    ANALYSIS_AI_VERDICT,
    ANALYSIS_PREDICTION,
    ANALYSIS_DEEP_SCAN,

    // TOOLS HUB
    TOOLS_POSITION_SIZER,
    TOOLS_TRADE_PLANNER,
    TOOLS_MTF_ANALYZER,
    TOOLS_BACKTESTER,
    TOOLS_PSYCHOLOGY,
    TOOLS_WHALE_TRACKER,
    TOOLS_ENTRY_ANALYZER,
    TOOLS_CORRELATION,

    // SIGNALS
    SIGNALS_LIST,
    SIGNALS_COMPOSER,
    SIGNALS_PERFORMANCE,

    // ALERTS
    ALERTS_LIST,
    ALERTS_COMPOSITE_BUILDER,
    ALERTS_PRIORITY,

    // SETTINGS
    SETTINGS_THEME,
    SETTINGS_LANGUAGE,
    SETTINGS_GLOSSARY,
    SETTINGS_TIER,
    SETTINGS_REPLAY_TUTORIAL
}

/**
 * One step in the guided tour.
 */
data class TourStep(
    val id: String,                          // "dash-1", "dash-2", ...
    val target: TutorialTargetId,
    val titleKey: Int,                       // R.string.tutorial_dash_1_title
    val messageKey: Int,                     // R.string.tutorial_dash_1_message
    val screenRoute: String,                 // "dashboard", "markets", ...
    val arrowDirection: ArrowDirection = ArrowDirection.AUTO,
    val tooltipPosition: TooltipPosition = TooltipPosition.AUTO
)

enum class ArrowDirection { TOP, BOTTOM, LEFT, RIGHT, AUTO }
enum class TooltipPosition { TOP, BOTTOM, LEFT, RIGHT, CENTER, AUTO }

/**
 * Sequence of tutorial steps grouped by screen.
 */
data class TourChapter(
    val screenRoute: String,
    val chapterTitleKey: Int,
    val steps: List<TourStep>
)
