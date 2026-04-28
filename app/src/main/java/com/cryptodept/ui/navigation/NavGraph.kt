package com.cryptodept.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.cryptodept.data.datastore.PreferencesManager
import com.cryptodept.ui.boot.BootSequenceScreen
import com.cryptodept.ui.settings.SettingsScreen
import com.cryptodept.ui.analysis.AnalysisScreen
import com.cryptodept.ui.dashboard.DashboardScreen
import com.cryptodept.ui.markets.MarketsScreen
import com.cryptodept.ui.charts.ChartsScreen
import com.cryptodept.ui.signals.SignalsScreen
import com.cryptodept.ui.alerts.AlertsScreen
import com.cryptodept.ui.feargreed.FearGreedScreen
import com.cryptodept.ui.indicators.IndicatorsScreen
import com.cryptodept.ui.coindetail.CoinDetailScreen
import com.cryptodept.ui.news.NewsScreen
import com.cryptodept.ui.news.NewsViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    preferencesManager: PreferencesManager // Hilt ще го подаде тук, но не ни трябва за SettingsScreen вече
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Boot.route
    ) {
        composable(Screen.Boot.route) {
            BootSequenceScreen(
                onBootComplete = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Boot.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(navController)
        }

        composable(Screen.Markets.route) {
            MarketsScreen(navController)
        }

        composable(
            route = Screen.Charts.route,
            arguments = listOf(navArgument("coinId") { type = NavType.StringType })
        ) { backStackEntry ->
            val coinId = backStackEntry.arguments?.getString("coinId") ?: "bitcoin"
            ChartsScreen(coinId)
        }

        composable(
            route = Screen.Analysis.route,
            arguments = listOf(navArgument("coinId") { type = NavType.StringType })
        ) { backStackEntry ->
            val coinId = backStackEntry.arguments?.getString("coinId") ?: "bitcoin"
            AnalysisScreen(
                coinId = coinId,
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Signals.route) {
            SignalsScreen()
        }

        composable(Screen.Alerts.route) {
            AlertsScreen()
        }

        // КОРИГИРАНО: Премахнат параметър prefs, SettingsScreen сам си взема ViewModel
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.FearGreed.route) {
            FearGreedScreen()
        }

        composable(Screen.Indicators.route) {
            IndicatorsScreen(navController)
        }

        composable(
            route = Screen.CoinDetail.route,
            arguments = listOf(navArgument("coinId") { type = NavType.StringType })
        ) { backStackEntry ->
            val coinId = backStackEntry.arguments?.getString("coinId") ?: "bitcoin"
            CoinDetailScreen(coinId)
        }

        composable(Screen.News.route) {
            val viewModel: NewsViewModel = hiltViewModel()
            NewsScreen(viewModel)
        }
    }
}