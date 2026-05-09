package com.cryptodept.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.cryptodept.ui.navigation.Screen
import com.cryptodept.ui.theme.LocalTerminalColors // REPLACED

@Composable
fun TerminalBottomBar(navController: NavHostController) {
    val items =
        listOf(
            Screen.Dashboard,
            Screen.Markets,
            Screen.Analysis,
            Screen.ToolsHub,
            Screen.News,
        )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val colors = LocalTerminalColors.current // OBTAIN CURRENT THEME COLORS

    // Don't show on boot screen
    if (currentRoute == Screen.Boot.route) return

    Column {
        // Top border line
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.primary.copy(alpha = 0.3f)),
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(Color.Black),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { screen ->
                val isSelected =
                    currentRoute == screen.route ||
                        (screen == Screen.Analysis && currentRoute?.startsWith("analysis") == true)

                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                onClickLabel = "Navigate to ${screen.label}",
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        val targetRoute =
                                            if (screen == Screen.Analysis) {
                                                Screen.Analysis.createRoute("bitcoin")
                                            } else {
                                                screen.route
                                            }

                                        navController.navigate(targetRoute) {
                                            popUpTo(Screen.Dashboard.route)
                                            launchSingleTop = true
                                        }
                                    }
                                },
                            ).padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = screen.icon,
                        fontSize = 20.sp,
                    )
                    Text(
                        text = screen.label,
                        color = if (isSelected) colors.primary else colors.dimText,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    )

                    if (isSelected) {
                        Box(
                            modifier =
                                Modifier
                                    .padding(top = 2.dp)
                                    .width(12.dp)
                                    .height(2.dp)
                                    .background(colors.primary),
                        )
                    }
                }
            }
        }
    }
}
