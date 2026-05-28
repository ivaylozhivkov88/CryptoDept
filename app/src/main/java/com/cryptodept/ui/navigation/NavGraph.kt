package com.cryptodept.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.cryptodept.data.datastore.PreferencesService

@Composable
fun NavGraph(
    navController: NavHostController,
    preferencesService: PreferencesService,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Boot.route,
    ) {
        // Modular NavGraphs (Q-006 Architectural Refactor)
        addAuthRoutes(navController, preferencesService)
        addCoreRoutes(navController)
        addTradingToolRoutes(navController)
        addIntelligenceRoutes(navController)
        addMacroRoutes(navController)
        addSystemRoutes(navController)
    }
}
