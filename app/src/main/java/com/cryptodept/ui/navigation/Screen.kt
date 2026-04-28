package com.cryptodept.ui.navigation

sealed class Screen(val route: String, val label: String = "", val icon: String = "") {
    object Boot : Screen("boot")
    object Dashboard : Screen("dashboard", "DASHBOARD", "🖥️")
    object Markets : Screen("markets", "MARKETS", "📊")
    object Analysis : Screen("analysis/{coinId}", "ANALYSIS", "📈") {
        fun createRoute(coinId: String) = "analysis/$coinId"
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
}
