package com.cryptodept.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.ui.navigation.Screen
import com.cryptodept.ui.theme.*
import com.cryptodept.ui.analysis.handleGlobalCommand
import com.cryptodept.ui.components.TerminalCommandBar
import com.cryptodept.viewmodel.SettingsViewModel

@Composable
fun ToolsHubScreen(
    navController: NavController,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val colors = LocalTerminalColors.current
    val isAdmin by settingsViewModel.isAdmin.collectAsState()

    val allTools =
        listOf(
            ToolItem("POSITION SIZER", "Risk-based calculator", Icons.Default.Build, Screen.PositionSizer.route),
            ToolItem("WHALE TRACKER", "On-chain activity", Icons.Default.Notifications, Screen.WhaleTracker.route),
            ToolItem("ENTRY ANALYZER", "Optimal entry zones", Icons.Default.Search, Screen.EntryAnalysis.route),
            ToolItem("MULTI-TIMEFRAME", "All TFs at a glance", Icons.AutoMirrored.Filled.List, Screen.MtfAnalysis.route),
            ToolItem("TRADE PLANNER", "Pre-trade checklist", Icons.Default.Check, Screen.TradePlanner.route),
            ToolItem("PSYCHOLOGY", "Tilt & emotion monitor", Icons.Default.Face, Screen.Psychology.route),
            ToolItem("JOURNAL", "Trade history", Icons.Default.Star, Screen.Journal.route),
            ToolItem("PERFORMANCE", "Personal trade stats", Icons.Default.ThumbUp, Screen.Performance.route),
            ToolItem("BACKTESTER", "Strategy simulation", Icons.Default.Refresh, Screen.Backtester.route),
            ToolItem("DEFI", "TVL & Yield monitor", Icons.Default.Info, Screen.DeFi.route),
            ToolItem("HALVING", "Bitcoin cycle analyzer", Icons.Default.DateRange, Screen.Seasonal.route),
            ToolItem("SIGNALS", "Signal composer", Icons.Default.Add, Screen.SignalComposer.route),
            ToolItem("CONTENT STUDIO", "AI Content Generator", Icons.Default.Create, Screen.ContentStudio.route, adminOnly = true),
            ToolItem("SETTINGS", "Terminal config", Icons.Default.Settings, Screen.Settings.route),
        )

    val tools = allTools.filter { !it.adminOnly || isAdmin }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(16.dp),
    ) {
        Text(
            text = ">>> TRADING TOOLS HUB",
            color = colors.primary,
            fontFamily = JetBrainsMono,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(tools) { tool ->
                ToolCard(tool) { navController.navigate(tool.route) }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // QUICK PSYCHOLOGY CHECK
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.amber)
                    .background(colors.amber.copy(alpha = 0.05f))
                    .padding(12.dp),
        ) {
            Column {
                Text(
                    "QUICK PSYCHOLOGY CHECK:",
                    color = colors.amber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = JetBrainsMono,
                )
                Text("Today: 4 trades | 1W / 3L | Tilt: 45/100", color = colors.textPrimary, fontSize = 12.sp, fontFamily = JetBrainsMono)
                Text("⚠ 3 consecutive losses — consider a break", color = colors.danger, fontSize = 10.sp, fontFamily = JetBrainsMono)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TerminalCommandBar(
            onCommandEntered = { cmd ->
                handleGlobalCommand(cmd, navController)
            },
            modifier = Modifier.imePadding()
        )
    }
}

@Composable
fun ToolCard(
    tool: ToolItem,
    onClick: () -> Unit,
) {
    val colors = LocalTerminalColors.current
    Column(
        modifier =
            Modifier
                .border(1.dp, colors.grid)
                .background(colors.background)
                .clickable { onClick() }
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = tool.icon,
            contentDescription = tool.name,
            tint = colors.primary,
            modifier = Modifier.size(32.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "[${tool.name}]",
            color = colors.primary,
            fontSize = 12.sp,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tool.description,
            color = colors.dimText,
            fontSize = 10.sp,
            fontFamily = JetBrainsMono,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

data class ToolItem(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val route: String,
    val adminOnly: Boolean = false,
)
