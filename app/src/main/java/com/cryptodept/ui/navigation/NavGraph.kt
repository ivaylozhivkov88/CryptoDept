package com.cryptodept.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.cryptodept.data.datastore.PreferencesService
import com.cryptodept.ui.alerts.AlertsScreen
import com.cryptodept.ui.analysis.AnalysisScreen
import com.cryptodept.ui.boot.BootSequenceScreen
import com.cryptodept.ui.briefing.DailyBriefingScreen
import com.cryptodept.ui.calendar.CalendarScreen
import com.cryptodept.ui.charts.ChartsScreen
import com.cryptodept.ui.coindetail.CoinDetailScreen
import com.cryptodept.ui.dashboard.DashboardScreen
import com.cryptodept.ui.derivatives.DerivativesScreen
import com.cryptodept.ui.feargreed.FearGreedScreen
import com.cryptodept.ui.indicators.IndicatorsScreen
import com.cryptodept.ui.journal.TradeJournalScreen
import com.cryptodept.ui.macro.MacroScreen
import com.cryptodept.ui.markets.MarketsScreen
import com.cryptodept.ui.news.NewsScreen
import com.cryptodept.ui.news.NewsViewModel
import com.cryptodept.ui.onboarding.OnboardingScreen
import com.cryptodept.ui.portfolio.PortfolioScreen
import com.cryptodept.ui.risk.RiskScoreScreen
import com.cryptodept.ui.search.SearchScreen
import com.cryptodept.ui.settings.SettingsScreen
import com.cryptodept.ui.signals.SignalsScreen
import com.cryptodept.ui.tools.*
import com.cryptodept.ui.tools.PsychologyScreen
import com.cryptodept.util.ProGate
import com.cryptodept.viewmodel.TradePlannerViewModel
import kotlinx.coroutines.launch

@Composable
fun NavGraph(
    navController: NavHostController,
    preferencesService: PreferencesService,
) {
    val isOnboardingComplete by preferencesService.isOnboardingComplete.collectAsState(initial = false)
    val scope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = Screen.Boot.route,
    ) {
        composable(Screen.Boot.route) {
            BootSequenceScreen(onBootComplete = {
                val nextRoute = if (isOnboardingComplete) Screen.Dashboard.route else Screen.Onboarding.route
                navController.navigate(nextRoute) {
                    popUpTo(Screen.Boot.route) { inclusive = true }
                }
            })
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(onOnboardingComplete = {
                scope.launch {
                    preferencesService.setOnboardingComplete(true)
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            })
        }

        composable(Screen.Dashboard.route) { DashboardScreen(navController) }
        composable(Screen.Markets.route) { MarketsScreen(navController) }

        composable(
            route = "analysis?coinId={coinId}",
            arguments =
                listOf(
                    navArgument("coinId") {
                        type = NavType.StringType
                        defaultValue = "bitcoin"
                    },
                ),
            deepLinks = listOf(
                androidx.navigation.navDeepLink { uriPattern = "cryptodept://analysis/{coinId}" }
            )
        ) { backStackEntry ->
            val coinId = backStackEntry.arguments?.getString("coinId") ?: "bitcoin"
            ProGate(
                feature = com.cryptodept.domain.tier.FeatureKey.ANALYSIS_ALL_COINS,
                onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "analysis", featureContext = key, onDismiss = { navController.popBackStack() }) }
            ) {
                AnalysisScreen(
                    coinId = coinId,
                    navController = navController,
                )
            }
        }

        composable(Screen.ToolsHub.route) {
            ToolsHubScreen(navController = navController)
        }

        // UPDATED TRADE PLANNER WITH ARGUMENTS
        composable(
            route = "trade_planner?coin={coin}&entry={entry}",
            arguments =
                listOf(
                    navArgument("coin") {
                        type = NavType.StringType
                        defaultValue = "BTC"
                    },
                    navArgument("entry") {
                        type = NavType.FloatType
                        defaultValue = 0f
                    },
                ),
        ) { backStackEntry ->
            val coin = backStackEntry.arguments?.getString("coin") ?: "BTC"
            val entry = backStackEntry.arguments?.getFloat("entry")?.toDouble() ?: 0.0

            val viewModel: TradePlannerViewModel = hiltViewModel()

            // If entry price provided, set it immediately
            if (entry > 0.0) {
                viewModel.setInitialParams(coin, entry, entry * 0.95, entry * 1.10)
            }

            ProGate(
                feature = com.cryptodept.domain.tier.FeatureKey.TRADE_PLANNER_BASIC,
                onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "backtester", featureContext = key, onDismiss = { navController.popBackStack() }) }
            ) {
                TradePlannerScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onSendToSizer = { e, sl, tp ->
                        navController.navigate(Screen.PositionSizer.route)
                    },
                )
            }
        }

        composable(Screen.EntryAnalysis.route) {
            ProGate(
                feature = com.cryptodept.domain.tier.FeatureKey.ENTRY_QUALITY_SCORER,
                onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "backtester", featureContext = key, onDismiss = { navController.popBackStack() }) }
            ) {
                EntryAnalyzerScreen(
                    onBack = { navController.popBackStack() },
                    onUseInPlanner = { coin, entry, sl, tp ->
                        navController.navigate("trade_planner?coin=$coin&entry=${entry.toFloat()}")
                    },
                    onNavigateToMarkets = {
                        navController.navigate(Screen.Markets.route)
                    }
                )
            }
        }

        composable(Screen.Portfolio.route) { 
            ProGate(
                feature = com.cryptodept.domain.tier.FeatureKey.PORTFOLIO_TRACKER_FULL,
                onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "watchlist", featureContext = key, onDismiss = { navController.popBackStack() }) }
            ) {
                PortfolioScreen(navController) 
            }
        }
        
        composable(
            route = "ai_coach?initialPrompt={initialPrompt}",
            arguments = listOf(navArgument("initialPrompt") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val initialPrompt = backStackEntry.arguments?.getString("initialPrompt")?.let {
                java.net.URLDecoder.decode(it, "UTF-8")
            }
            ProGate(
                feature = com.cryptodept.domain.tier.FeatureKey.DAILY_BRIEFING,
                onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "ai_narrative", featureContext = key, onDismiss = { navController.popBackStack() }) }
            ) {
                com.cryptodept.ui.ai.AICoachScreen(
                    initialPrompt = initialPrompt
                )
            }
        }

        composable(Screen.MtfAnalysis.route) {
            ProGate(
                feature = com.cryptodept.domain.tier.FeatureKey.MULTI_TIMEFRAME,
                onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "analysis", featureContext = key, onDismiss = { navController.popBackStack() }) }
            ) {
                MTFScreen(
                    onBack = { navController.popBackStack() },
                    onGoToDashboard = { navController.navigate(Screen.Dashboard.route) },
                    onNavigateToMarkets = { navController.navigate(Screen.Markets.route) }
                )
            }
        }

        composable(Screen.PositionSizer.route) {
            PositionSizeScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.WhaleTracker.route) {
            ProGate(
                feature = com.cryptodept.domain.tier.FeatureKey.WHALE_TRACKER,
                onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "whale_tracker", featureContext = key, onDismiss = { navController.popBackStack() }) }
            ) {
                WhaleTrackerScreen(onBack = { navController.popBackStack() })
            }
        }

        composable(Screen.AgentHub.route) {
            com.cryptodept.ui.agents.AgentHubScreen(navController = navController)
        }

        composable(Screen.Prediction.route) {
            val settingsViewModel: com.cryptodept.viewmodel.SettingsViewModel = hiltViewModel()
            val tier by settingsViewModel.tierAccessManager.currentTier.collectAsState()
            
            if (tier == com.cryptodept.domain.tier.AccessTier.ADMIN) {
                com.cryptodept.ui.prediction.PredictionHubScreen(onBack = { navController.popBackStack() })
            } else {
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }

        composable(Screen.ContentStudio.route) {
            val settingsViewModel: com.cryptodept.viewmodel.SettingsViewModel = hiltViewModel()
            val tier by settingsViewModel.tierAccessManager.currentTier.collectAsState()
            
            if (tier == com.cryptodept.domain.tier.AccessTier.ADMIN) {
                com.cryptodept.ui.ai.ContentStudioScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToAiCoach = { prompt ->
                        val encoded = java.net.URLEncoder.encode(prompt, "UTF-8")
                        navController.navigate("ai_coach?initialPrompt=$encoded")
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }

        composable(Screen.Risk.route) { 
            ProGate(
                feature = com.cryptodept.domain.tier.FeatureKey.RISK_SCORING,
                onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "general", featureContext = key, onDismiss = { navController.popBackStack() }) }
            ) {
                RiskScoreScreen() 
            }
        }
        
        composable(Screen.Psychology.route) { 
            ProGate(
                feature = com.cryptodept.domain.tier.FeatureKey.PSYCHOLOGY_LOCK,
                onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "general", featureContext = key, onDismiss = { navController.popBackStack() }) }
            ) {
                PsychologyScreen(onBack = { navController.popBackStack() }) 
            }
        }
        
        composable(Screen.Correlation.route) { 
            ProGate(
                feature = com.cryptodept.domain.tier.FeatureKey.CORRELATION_MATRIX,
                onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "general", featureContext = key, onDismiss = { navController.popBackStack() }) }
            ) {
                CorrelationScreen(onBack = { navController.popBackStack() }) 
            }
        }
        
        composable(Screen.Briefing.route) { 
            ProGate(
                feature = com.cryptodept.domain.tier.FeatureKey.DAILY_BRIEFING,
                onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "ai_narrative", featureContext = key, onDismiss = { navController.popBackStack() }) }
            ) {
                DailyBriefingScreen() 
            }
        }
        
        composable(Screen.Derivatives.route) { 
            ProGate(
                feature = com.cryptodept.domain.tier.FeatureKey.DERIVATIVES_DATA,
                onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "derivatives", featureContext = key, onDismiss = { navController.popBackStack() }) }
            ) {
                DerivativesScreen() 
            }
        }
        
        composable(Screen.Calendar.route) { 
            ProGate(
                feature = com.cryptodept.domain.tier.FeatureKey.CALENDAR,
                onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "general", featureContext = key, onDismiss = { navController.popBackStack() }) }
            ) {
                CalendarScreen() 
            }
        }
        
        composable(Screen.Macro.route) { 
            ProGate(
                feature = com.cryptodept.domain.tier.FeatureKey.MACRO_INDICATORS,
                onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "general", featureContext = key, onDismiss = { navController.popBackStack() }) }
            ) {
                MacroScreen() 
            }
        }
        
        composable(Screen.Alerts.route) {
            AlertsScreen(
                onNavigateToBuilder = { navController.navigate("alert_builder") },
                onNavigateToPaywall = { key -> navController.navigateToPaywall("alerts", key) }
            )
        }
        
        composable("alert_builder") {
            ProGate(
                feature = com.cryptodept.domain.tier.FeatureKey.COMPOSITE_ALERTS,
                onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "alerts", featureContext = key, onDismiss = { navController.popBackStack() }) }
            ) {
                com.cryptodept.ui.alerts.builder.CompositeAlertBuilderScreen(onBack = { navController.popBackStack() })
            }
        }
        
        composable(Screen.FearGreed.route) { FearGreedScreen() }
        composable(Screen.Indicators.route) { 
            ProGate(
                feature = com.cryptodept.domain.tier.FeatureKey.MARKETS_FILTERS,
                onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "markets", featureContext = key, onDismiss = { navController.popBackStack() }) }
            ) {
                IndicatorsScreen(navController) 
            }
        }
        
        composable(Screen.Backtester.route) {
            ProGate(
                feature = com.cryptodept.domain.tier.FeatureKey.BACKTESTER,
                onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "backtester", featureContext = key, onDismiss = { navController.popBackStack() }) }
            ) {
                BacktesterScreen(onBack = { navController.popBackStack() })
            }
        }

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

        composable(Screen.Signals.route) { 
            ProGate(
                feature = com.cryptodept.domain.tier.FeatureKey.DASHBOARD_SENTIMENT_MATRIX,
                onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "ai_narrative", featureContext = key, onDismiss = { navController.popBackStack() }) }
            ) {
                SignalsScreen() 
            }
        }
        
        composable(Screen.News.route) {
            val viewModel: NewsViewModel = hiltViewModel()
            NewsScreen(viewModel = viewModel)
        }
        
        composable(Screen.Settings.route) { SettingsScreen(onBack = { navController.popBackStack() }, navController = navController) }
        
        composable(Screen.Journal.route) { 
            ProGate(
                feature = com.cryptodept.domain.tier.FeatureKey.TRADE_JOURNAL_UNLIMITED,
                onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "general", featureContext = key, onDismiss = { navController.popBackStack() }) }
            ) {
                TradeJournalScreen() 
            }
        }
        
        composable(Screen.SignalComposer.route) {
            ProGate(
                feature = com.cryptodept.domain.tier.FeatureKey.CONTENT_STUDIO,
                onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "general", featureContext = key, onDismiss = { navController.popBackStack() }) }
            ) {
                com.cryptodept.ui.signals.composer.SignalComposerScreen(onBack = { navController.popBackStack() })
            }
        }
        
        composable(Screen.Seasonal.route) {
            ProGate(
                feature = com.cryptodept.domain.tier.FeatureKey.SEASONAL_PATTERNS,
                onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "general", featureContext = key, onDismiss = { navController.popBackStack() }) }
            ) {
                com.cryptodept.ui.seasonal.SeasonalScreen(onBack = { navController.popBackStack() })
            }
        }
        
        composable(Screen.DeFi.route) {
            ProGate(
                feature = com.cryptodept.domain.tier.FeatureKey.DEFI_YIELDS,
                onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "defi", featureContext = key, onDismiss = { navController.popBackStack() }) }
            ) {
                com.cryptodept.ui.defi.DeFiScreen(onBack = { navController.popBackStack() })
            }
        }
        
        composable(Screen.Performance.route) {
            ProGate(
                feature = com.cryptodept.domain.tier.FeatureKey.ENTRY_QUALITY_SCORER,
                onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "general", featureContext = key, onDismiss = { navController.popBackStack() }) }
            ) {
                com.cryptodept.ui.performance.PerformanceScreen(onBack = { navController.popBackStack() })
            }
        }
        
        composable(Screen.Achievements.route) {
            val viewModel: com.cryptodept.viewmodel.AchievementsViewModel = hiltViewModel()
            com.cryptodept.ui.achievements.AchievementsScreen(
                engine = viewModel.engine,
                onBack = { navController.popBackStack() },
            )
        }

        composable("accuracy_dashboard") {
            com.cryptodept.ui.settings.AccuracyDashboardScreen(onBack = { navController.popBackStack() })
        }
        
        composable(
            route = Screen.Comparison.route,
            arguments =
                listOf(
                    navArgument("coin1") {
                        type = NavType.StringType
                        defaultValue = "bitcoin"
                    },
                    navArgument("coin2") {
                        type = NavType.StringType
                        defaultValue = "ethereum"
                    },
                ),
        ) { backStackEntry ->
            val c1 = backStackEntry.arguments?.getString("coin1") ?: "bitcoin"
            val c2 = backStackEntry.arguments?.getString("coin2") ?: "ethereum"
            ProGate(
                feature = com.cryptodept.domain.tier.FeatureKey.COMPARISON_TOOL,
                onLocked = { key -> com.cryptodept.ui.paywall.PaywallScreen(reason = "general", featureContext = key, onDismiss = { navController.popBackStack() }) }
            ) {
                com.cryptodept.ui.comparison.ComparisonScreen(coin1Id = c1, coin2Id = c2, onBack = { navController.popBackStack() })
            }
        }

        composable(Screen.Glossary.route) {
            com.cryptodept.ui.education.GlossaryScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Search.route) {
            SearchScreen(navController)
        }

        composable(
            route = Screen.Paywall.route,
            arguments = listOf(
                navArgument("reason") {
                    type = NavType.StringType
                    defaultValue = "general"
                    nullable = true
                },
                navArgument("featureKey") {
                    type = NavType.StringType
                    defaultValue = null
                    nullable = true
                }
            ),
            deepLinks = listOf(
                androidx.navigation.navDeepLink { uriPattern = "cryptodept://paywall?reason={reason}&featureKey={featureKey}" }
            )
        ) { backStackEntry ->
            val reason = backStackEntry.arguments?.getString("reason") ?: "general"
            val featureKeyStr = backStackEntry.arguments?.getString("featureKey")
            val featureKey = featureKeyStr?.let { 
                try { com.cryptodept.domain.tier.FeatureKey.valueOf(it) } catch (e: Exception) { null }
            }
            
            com.cryptodept.ui.paywall.PaywallScreen(
                reason = reason,
                featureContext = featureKey,
                onDismiss = { navController.popBackStack() }
            )
        }
    }
}
