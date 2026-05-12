package com.cryptodept.ui.navigation

sealed class Screen(
    val route: String,
    val label: String = "",
    val icon: String = "",
) {
    object Boot : Screen("boot")

    object Dashboard : Screen("dashboard", "DASHBOARD", "🖥️")

    object Markets : Screen("markets", "MARKETS", "📊")

    object Analysis : Screen("analysis", "ANALYSIS", "📈") {
        fun createRoute(coinId: String) = "analysis?coinId=$coinId"
    }

    object Signals : Screen("signals", "SIGNALS", "⚡")

    object News : Screen("news", "NEWS", "📰")

    object Charts : Screen("charts/{coinId}") {
        fun createRoute(coinId: String) = "charts/$coinId"
    }

    object Alerts : Screen("alerts")

    object Settings : Screen("settings")

    object FearGreed : Screen("fear_greed")

    object Indicators : Screen("indicators", "SCANNER", "🔍")

    object CoinDetail : Screen("coin_detail/{coinId}") {
        fun createRoute(coinId: String) = "coin_detail/$coinId"
    }

    // BLOCK C: NEW SCREENS
    object Risk : Screen("risk", "RISK", "⚠")

    object Briefing : Screen("briefing", "BRIEFING", "📋")

    object Derivatives : Screen("derivatives", "DERIVATIVES", "⛓")

    object Journal : Screen("journal", "JOURNAL", "📓")

    object Calendar : Screen("calendar", "CALENDAR", "📅")

    object Macro : Screen("macro", "MACRO", "🌍")

    object Correlation : Screen("correlation", "MATRIX", "🔳")

    object Seasonal : Screen("seasonal", "SEASONAL", "⏳")

    object DeFi : Screen("defi", "DEFI", "🏦")

    object Comparison : Screen("comparison?coin1={coin1}&coin2={coin2}", "COMPARE", "⚔") {
        fun createRoute(
            coin1: String,
            coin2: String,
        ) = "comparison?coin1=$coin1&coin2=$coin2"
    }

    // BLOCK E: PROFESSIONAL TRADER TOOLS
    object ToolsHub : Screen("tools_hub", "TOOLS", "🛠️")

    object PositionSizer : Screen("position_sizer")

    object MtfAnalysis : Screen("mtf_analysis")

    object TradePlanner : Screen("trade_planner")

    object Psychology : Screen("psychology")

    object EntryAnalysis : Screen("entry_analysis")

    object WhaleTracker : Screen("whale_tracker")

    // BLOCK F: SUPREME MODE
    object Prediction : Screen("prediction")

    object Portfolio : Screen("portfolio")

    object AICoach : Screen("ai_coach")

    object Onboarding : Screen("onboarding")

    object Backtester : Screen("backtester")

    object SignalComposer : Screen("signal_composer")

    object Performance : Screen("performance", "PERFORMANCE", "📈")

    object ContentStudio : Screen("content_studio")

    object Achievements : Screen("achievements", "ACHIEVEMENTS", "🏆")

    object AgentHub : Screen("agent_hub", "AGENT HUB", "🤖")
}
