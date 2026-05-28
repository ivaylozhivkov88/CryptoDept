package com.cryptodept.ui.navigation

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.cryptodept.domain.tier.AccessTier
import com.cryptodept.domain.tier.FeatureKey
import com.cryptodept.ui.alerts.AlertsScreen
import com.cryptodept.ui.analysis.AnalysisScreen
import com.cryptodept.ui.charts.ChartsScreen
import com.cryptodept.ui.coindetail.CoinDetailScreen
import com.cryptodept.ui.dashboard.DashboardScreen
import com.cryptodept.ui.news.NewsScreen
import com.cryptodept.ui.news.NewsViewModel
import com.cryptodept.ui.settings.SettingsScreen
import com.cryptodept.ui.tools.*
import com.cryptodept.util.ProGate
import com.cryptodept.viewmodel.TradePlannerViewModel
import kotlinx.coroutines.launch

/**
 * Modular NavGraphs for CryptoDept Terminal.
 * Part of Q-006 Architectural Refactor.
 */

fun NavGraphBuilder.addAuthRoutes(
    navController: NavHostController,
    preferencesService: com.cryptodept.data.datastore.PreferencesService
) {
    composable(Screen.Boot.route) {
        val isOnboardingComplete by preferencesService.isOnboardingComplete.collectAsState(initial = false)
        com.cryptodept.ui.boot.BootSequenceScreen(onBootComplete = {
            val nextRoute = if (isOnboardingComplete) Screen.Dashboard.route else Screen.Onboarding.route
            navController.navigate(nextRoute) {
                popUpTo(Screen.Boot.route) { inclusive = true }
            }
        })
    }

    composable(Screen.Onboarding.route) {
        val scope = rememberCoroutineScope()
        com.cryptodept.ui.onboarding.OnboardingScreen(onOnboardingComplete = {
            scope.launch {
                preferencesService.setOnboardingComplete(true)
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            }
        })
    }
}

fun NavGraphBuilder.addCoreRoutes(navController: NavHostController) {
    composable(Screen.Dashboard.route) { DashboardScreen(navController) }
    composable(Screen.Markets.route) { com.cryptodept.ui.markets.MarketsScreen(navController) }
    composable(Screen.Search.route) { com.cryptodept.ui.search.SearchScreen(navController) }
    
    composable(
        route = Screen.Charts.route,
        arguments = listOf(navArgument("coinId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val coinId = backStackEntry.arguments?.getString("coinId") ?: "bitcoin"
        ChartsScreen(coinId = coinId)
    }

    composable(
        route = Screen.CoinDetail.route,
        arguments = listOf(navArgument("coinId") { type = NavType.StringType }),
    ) { backStackEntry ->
        val coinId = backStackEntry.arguments?.getString("coinId") ?: "bitcoin"
        CoinDetailScreen(coinId = coinId)
    }
}

fun NavGraphBuilder.addTradingToolRoutes(navController: NavHostController) {
    composable(Screen.ToolsHub.route) {
        ToolsHubScreen(navController = navController)
    }

    composable(
        route = "trade_planner?coin={coin}&entry={entry}",
        arguments = listOf(
            navArgument("coin") { type = NavType.StringType; defaultValue = "BTC" },
            navArgument("entry") { type = NavType.FloatType; defaultValue = 0f }
        ),
    ) { backStackEntry ->
        val coin = backStackEntry.arguments?.getString("coin") ?: "BTC"
        val entry = backStackEntry.arguments?.getFloat("entry")?.toDouble() ?: 0.0
        val viewModel: TradePlannerViewModel = hiltViewModel()
        if (entry > 0.0) { viewModel.setInitialParams(coin, entry, entry * 0.95, entry * 1.10) }

        ProGate(
            feature = FeatureKey.TRADE_PLANNER_BASIC,
            onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "backtester", featureContext = key, onDismiss = { navController.popBackStack() }) }
        ) {
            TradePlannerScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSendToSizer = { _, _, _ -> navController.navigate(Screen.PositionSizer.route) }
            )
        }
    }

    composable(Screen.EntryAnalysis.route) {
        ProGate(
            feature = FeatureKey.ENTRY_QUALITY_SCORER,
            onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "backtester", featureContext = key, onDismiss = { navController.popBackStack() }) }
        ) {
            EntryAnalyzerScreen(
                onBack = { navController.popBackStack() },
                onUseInPlanner = { coin, entry, _, _ -> navController.navigate("trade_planner?coin=$coin&entry=${entry.toFloat()}") },
                onNavigateToMarkets = { navController.navigate(Screen.Markets.route) }
            )
        }
    }

    composable(Screen.PositionSizer.route) { PositionSizeScreen(onBack = { navController.popBackStack() }) }
    
    composable(Screen.Backtester.route) {
        ProGate(
            feature = FeatureKey.BACKTESTER,
            onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "backtester", featureContext = key, onDismiss = { navController.popBackStack() }) }
        ) {
            BacktesterScreen(onBack = { navController.popBackStack() })
        }
    }

    composable(Screen.Journal.route) {
        ProGate(
            feature = FeatureKey.TRADE_JOURNAL_UNLIMITED,
            onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "general", featureContext = key, onDismiss = { navController.popBackStack() }) }
        ) {
            com.cryptodept.ui.journal.TradeJournalScreen()
        }
    }
}

fun NavGraphBuilder.addIntelligenceRoutes(navController: NavHostController) {
    composable(
        route = "analysis?coinId={coinId}",
        arguments = listOf(navArgument("coinId") { type = NavType.StringType; defaultValue = "bitcoin" }),
        deepLinks = listOf(androidx.navigation.navDeepLink { uriPattern = "cryptodept://analysis/{coinId}" })
    ) { backStackEntry ->
        val coinId = backStackEntry.arguments?.getString("coinId") ?: "bitcoin"
        ProGate(
            feature = FeatureKey.ANALYSIS_ALL_COINS,
            onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "analysis", featureContext = key, onDismiss = { navController.popBackStack() }) }
        ) {
            AnalysisScreen(coinId = coinId, navController = navController)
        }
    }

    composable(Screen.MtfAnalysis.route) {
        ProGate(
            feature = FeatureKey.MULTI_TIMEFRAME,
            onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "analysis", featureContext = key, onDismiss = { navController.popBackStack() }) }
        ) {
            MTFScreen(
                onBack = { navController.popBackStack() },
                onGoToDashboard = { navController.navigate(Screen.Dashboard.route) },
                onNavigateToMarkets = { navController.navigate(Screen.Markets.route) }
            )
        }
    }

    composable("agent_hub") { com.cryptodept.ui.agents.AgentHubScreen(navController = navController) }
    
    composable(Screen.WhaleTracker.route) {
        ProGate(
            feature = FeatureKey.WHALE_TRACKER,
            onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "whale_tracker", featureContext = key, onDismiss = { navController.popBackStack() }) }
        ) {
            com.cryptodept.ui.tools.WhaleTrackerScreen(onBack = { navController.popBackStack() })
        }
    }
    
    composable(Screen.Prediction.route) {
        val settingsViewModel: com.cryptodept.viewmodel.SettingsViewModel = hiltViewModel()
        val tier by settingsViewModel.tierAccessManager.currentTier.collectAsState()
        if (tier == AccessTier.ADMIN) {
            com.cryptodept.ui.prediction.PredictionHubScreen(onBack = { navController.popBackStack() })
        } else {
            LaunchedEffect(Unit) { navController.popBackStack() }
        }
    }

    composable(Screen.ContentStudio.route) {
        val settingsViewModel: com.cryptodept.viewmodel.SettingsViewModel = hiltViewModel()
        val tier by settingsViewModel.tierAccessManager.currentTier.collectAsState()
        if (tier == AccessTier.ADMIN) {
            com.cryptodept.ui.ai.ContentStudioScreen(
                onBack = { navController.popBackStack() },
                onNavigateToAiCoach = { prompt ->
                    val encoded = java.net.URLEncoder.encode(prompt, "UTF-8")
                    navController.navigate("ai_coach?initialPrompt=$encoded")
                }
            )
        } else {
            LaunchedEffect(Unit) { navController.popBackStack() }
        }
    }

    composable(
        route = "ai_coach?initialPrompt={initialPrompt}",
        arguments = listOf(navArgument("initialPrompt") { type = NavType.StringType; nullable = true; defaultValue = null })
    ) { backStackEntry ->
        val initialPrompt = backStackEntry.arguments?.getString("initialPrompt")?.let { java.net.URLDecoder.decode(it, "UTF-8") }
        ProGate(
            feature = FeatureKey.DAILY_BRIEFING,
            onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "ai_narrative", featureContext = key, onDismiss = { navController.popBackStack() }) }
        ) {
            com.cryptodept.ui.ai.AICoachScreen(initialPrompt = initialPrompt)
        }
    }

    composable(Screen.Briefing.route) {
        ProGate(
            feature = FeatureKey.DAILY_BRIEFING,
            onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "ai_narrative", featureContext = key, onDismiss = { navController.popBackStack() }) }
        ) {
            com.cryptodept.ui.briefing.DailyBriefingScreen()
        }
    }

    composable(Screen.Signals.route) {
        ProGate(
            feature = FeatureKey.WHALE_TRACKER, // Use Whale Tracker as proxy for Signals
            onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "general", featureContext = key, onDismiss = { navController.popBackStack() }) }
        ) {
            com.cryptodept.ui.signals.SignalsScreen()
        }
    }

    composable(Screen.News.route) {
        val viewModel: NewsViewModel = hiltViewModel()
        NewsScreen(viewModel = viewModel)
    }
}

fun NavGraphBuilder.addMacroRoutes(navController: NavHostController) {
    composable(Screen.Risk.route) {
        ProGate(
            feature = FeatureKey.RISK_SCORING,
            onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "general", featureContext = key, onDismiss = { navController.popBackStack() }) }
        ) {
            com.cryptodept.ui.risk.RiskScoreScreen()
        }
    }

    composable(Screen.Psychology.route) {
        ProGate(
            feature = FeatureKey.PSYCHOLOGY_LOCK,
            onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "general", featureContext = key, onDismiss = { navController.popBackStack() }) }
        ) {
            PsychologyScreen(onBack = { navController.popBackStack() })
        }
    }

    composable(Screen.Correlation.route) {
        ProGate(
            feature = FeatureKey.CORRELATION_MATRIX,
            onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "general", featureContext = key, onDismiss = { navController.popBackStack() }) }
        ) {
            com.cryptodept.ui.tools.CorrelationScreen(onBack = { navController.popBackStack() })
        }
    }

    composable(Screen.Derivatives.route) {
        ProGate(
            feature = FeatureKey.DERIVATIVES_DATA,
            onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "derivatives", featureContext = key, onDismiss = { navController.popBackStack() }) }
        ) {
            com.cryptodept.ui.derivatives.DerivativesScreen()
        }
    }

    composable(Screen.Calendar.route) {
        ProGate(
            feature = FeatureKey.CALENDAR,
            onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "general", featureContext = key, onDismiss = { navController.popBackStack() }) }
        ) {
            com.cryptodept.ui.calendar.CalendarScreen()
        }
    }

    composable(Screen.Macro.route) {
        ProGate(
            feature = FeatureKey.MACRO_INDICATORS,
            onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "general", featureContext = key, onDismiss = { navController.popBackStack() }) }
        ) {
            com.cryptodept.ui.macro.MacroScreen()
        }
    }

    composable(Screen.DeFi.route) {
        ProGate(
            feature = FeatureKey.DEFI_YIELDS,
            onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "defi", featureContext = key, onDismiss = { navController.popBackStack() }) }
        ) {
            com.cryptodept.ui.defi.DeFiScreen(onBack = { navController.popBackStack() })
        }
    }

    composable(Screen.FearGreed.route) { com.cryptodept.ui.feargreed.FearGreedScreen() }
}

fun NavGraphBuilder.addSystemRoutes(navController: NavHostController) {
    composable(Screen.Settings.route) { SettingsScreen(onBack = { navController.popBackStack() }, navController = navController) }
    
    composable(Screen.Alerts.route) {
        AlertsScreen(
            onNavigateToBuilder = { navController.navigate("alert_builder") },
            onNavigateToPaywall = { key -> navController.navigateToPaywall("alerts", key) }
        )
    }

    composable("alert_builder") {
        ProGate(
            feature = FeatureKey.COMPOSITE_ALERTS,
            onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "alerts", featureContext = key, onDismiss = { navController.popBackStack() }) }
        ) {
            com.cryptodept.ui.alerts.builder.CompositeAlertBuilderScreen(onBack = { navController.popBackStack() })
        }
    }

    composable(Screen.Portfolio.route) {
        ProGate(
            feature = FeatureKey.PORTFOLIO_TRACKER_FULL,
            onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "watchlist", featureContext = key, onDismiss = { navController.popBackStack() }) }
        ) {
            com.cryptodept.ui.portfolio.PortfolioScreen(navController)
        }
    }

    composable(Screen.SignalComposer.route) {
        ProGate(
            feature = FeatureKey.CONTENT_STUDIO,
            onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "general", featureContext = key, onDismiss = { navController.popBackStack() }) }
        ) {
            com.cryptodept.ui.signals.composer.SignalComposerScreen(onBack = { navController.popBackStack() })
        }
    }

    composable(Screen.Seasonal.route) {
        ProGate(
            feature = FeatureKey.SEASONAL_PATTERNS,
            onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "general", featureContext = key, onDismiss = { navController.popBackStack() }) }
        ) {
            com.cryptodept.ui.seasonal.SeasonalScreen(onBack = { navController.popBackStack() })
        }
    }

    composable(Screen.Performance.route) {
        ProGate(
            feature = FeatureKey.ENTRY_QUALITY_SCORER,
            onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "general", featureContext = key, onDismiss = { navController.popBackStack() }) }
        ) {
            com.cryptodept.ui.performance.PerformanceScreen(onBack = { navController.popBackStack() })
        }
    }

    composable(Screen.Achievements.route) {
        ProGate(
            feature = FeatureKey.ACHIEVEMENTS,
            onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "general", featureContext = key, onDismiss = { navController.popBackStack() }) }
        ) {
            val viewModel: com.cryptodept.viewmodel.AchievementsViewModel = hiltViewModel()
            com.cryptodept.ui.achievements.AchievementsScreen(engine = viewModel.engine, onBack = { navController.popBackStack() })
        }
    }

    composable("accuracy_dashboard") {
        com.cryptodept.ui.settings.AccuracyDashboardScreen(onBack = { navController.popBackStack() })
    }

    composable(Screen.Glossary.route) {
        com.cryptodept.ui.education.GlossaryScreen(onBack = { navController.popBackStack() })
    }

    composable(Screen.Paywall.route, arguments = listOf(
        navArgument("reason") { type = NavType.StringType; defaultValue = "general"; nullable = true },
        navArgument("featureKey") { type = NavType.StringType; defaultValue = null; nullable = true }
    )) { backStackEntry ->
        val reason = backStackEntry.arguments?.getString("reason") ?: "general"
        val featureKeyStr = backStackEntry.arguments?.getString("featureKey")
        val featureKey = featureKeyStr?.let { try { FeatureKey.valueOf(it) } catch (e: Exception) { null } }
        com.cryptodept.ui.paywall.PaywallScreen(reason = reason, featureContext = featureKey, onDismiss = { navController.popBackStack() })
    }

    composable(
        route = Screen.Comparison.route,
        arguments = listOf(
            navArgument("coin1") { type = NavType.StringType; defaultValue = "bitcoin" },
            navArgument("coin2") { type = NavType.StringType; defaultValue = "ethereum" }
        ),
    ) { backStackEntry ->
        val c1 = backStackEntry.arguments?.getString("coin1") ?: "bitcoin"
        val c2 = backStackEntry.arguments?.getString("coin2") ?: "ethereum"
        ProGate(
            feature = FeatureKey.COMPARISON_TOOL,
            onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "general", featureContext = key, onDismiss = { navController.popBackStack() }) }
        ) {
            com.cryptodept.ui.comparison.ComparisonScreen(coin1Id = c1, coin2Id = c2, onBack = { navController.popBackStack() })
        }
    }
}
