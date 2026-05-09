package com.cryptodept.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
        ) { backStackEntry ->
            val coinId = backStackEntry.arguments?.getString("coinId") ?: "bitcoin"
            AnalysisScreen(
                coinId = coinId,
                navController = navController,
            )
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

            TradePlannerScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSendToSizer = { e, sl, tp ->
                    // Future: pass entry, sl, tp as navigation arguments
                    navController.navigate(Screen.PositionSizer.route)
                },
            )
        }

        composable(Screen.EntryAnalysis.route) {
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

        composable(Screen.Portfolio.route) { PortfolioScreen(navController) }
        composable(Screen.AICoach.route) {
            ProGate(onLocked = {
                com.cryptodept.ui.paywall
                    .PaywallScreen()
            }) {
                com.cryptodept.ui.ai
                    .AICoachScreen()
            }
        }

        composable(Screen.MtfAnalysis.route) {
            MTFScreen(
                onBack = { navController.popBackStack() },
                onGoToDashboard = { navController.navigate(Screen.Dashboard.route) },
                onNavigateToMarkets = { navController.navigate(Screen.Markets.route) }
            )
        }

        composable(Screen.PositionSizer.route) {
            PositionSizeScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.WhaleTracker.route) {
            WhaleTrackerScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.ContentStudio.route) {
            val settingsViewModel: com.cryptodept.viewmodel.SettingsViewModel = hiltViewModel()
            val isAdmin by settingsViewModel.isAdmin.collectAsState()
            
            if (isAdmin) {
                com.cryptodept.ui.ai.ContentStudioScreen(onBack = { navController.popBackStack() })
            } else {
                // Access denied UI or redirect
                Box(modifier = androidx.compose.ui.Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    androidx.compose.material3.Text("ADMIN ACCESS REQUIRED", color = androidx.compose.ui.graphics.Color.Red)
                }
            }
        }

        composable(Screen.Risk.route) { RiskScoreScreen() }
        composable(Screen.Psychology.route) { PsychologyScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Correlation.route) { CorrelationScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Briefing.route) { DailyBriefingScreen() }
        composable(Screen.Derivatives.route) { DerivativesScreen() }
        composable(Screen.Calendar.route) { CalendarScreen() }
        composable(Screen.Macro.route) { MacroScreen() }
        composable(Screen.Alerts.route) {
            AlertsScreen(onNavigateToBuilder = { navController.navigate("alert_builder") })
        }
        composable("alert_builder") {
            com.cryptodept.ui.alerts.builder
                .CompositeAlertBuilderScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.FearGreed.route) { FearGreedScreen() }
        composable(Screen.Indicators.route) { IndicatorsScreen(navController) }
        composable(Screen.Backtester.route) {
            ProGate(onLocked = {
                com.cryptodept.ui.paywall
                    .PaywallScreen()
            }) {
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

        // Всички останали дестинации остават същите...
        composable(Screen.Signals.route) { SignalsScreen() }
        composable(Screen.News.route) {
            val viewModel: NewsViewModel = hiltViewModel()
            NewsScreen(viewModel = viewModel)
        }
        composable(Screen.Settings.route) { SettingsScreen(onBack = { navController.popBackStack() }) }
        composable(Screen.Journal.route) { TradeJournalScreen() }
        composable(Screen.SignalComposer.route) {
            com.cryptodept.ui.signals.composer
                .SignalComposerScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Seasonal.route) {
            com.cryptodept.ui.seasonal
                .SeasonalScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.DeFi.route) {
            com.cryptodept.ui.defi
                .DeFiScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Performance.route) {
            com.cryptodept.ui.performance
                .PerformanceScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Achievements.route) {
            val viewModel: com.cryptodept.viewmodel.AchievementsViewModel = hiltViewModel()
            com.cryptodept.ui.achievements.AchievementsScreen(
                engine = viewModel.engine,
                onBack = { navController.popBackStack() },
            )
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
            com.cryptodept.ui.comparison
                .ComparisonScreen(coin1Id = c1, coin2Id = c2, onBack = { navController.popBackStack() })
        }
    }
}
