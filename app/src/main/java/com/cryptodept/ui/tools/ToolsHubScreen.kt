package com.cryptodept.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.ui.tutorial.tutorialTarget
import com.cryptodept.domain.tutorial.TutorialTargetId
import com.cryptodept.ui.navigation.Screen
import com.cryptodept.ui.navigation.navigateToPaywall
import com.cryptodept.ui.theme.*
import com.cryptodept.viewmodel.SettingsViewModel
import com.cryptodept.viewmodel.ToolsHubViewModel
import com.cryptodept.domain.tier.FeatureKey
import com.cryptodept.domain.tier.AccessTier
import com.cryptodept.ui.components.FeatureHelpIcon

import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ToolsHubScreen(
    navController: NavController,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    viewModel: ToolsHubViewModel = hiltViewModel()
) {
    val colors = LocalTerminalColors.current
    val currentTier by viewModel.tierAccessManager.currentTier.collectAsStateWithLifecycle()
    
    val essentialTools = listOf(
        ToolItem("POSITION SIZER", "Lot size & risk calc", Icons.Default.Build, Screen.PositionSizer.route, targetId = TutorialTargetId.TOOLS_POSITION_SIZER, feature = FeatureKey.POSITION_SIZER),
        ToolItem("TRADE PLANNER", "Pre-trade checklist", Icons.Default.Check, Screen.TradePlanner.route, targetId = TutorialTargetId.TOOLS_TRADE_PLANNER, feature = FeatureKey.TRADE_PLANNER_BASIC),
        ToolItem("MTF ANALYZER", "All TFs overview", Icons.AutoMirrored.Filled.List, Screen.MtfAnalysis.route, targetId = TutorialTargetId.TOOLS_MTF_ANALYZER, feature = FeatureKey.MULTI_TIMEFRAME),
        ToolItem("WHALE TRACKER", "On-chain flow", Icons.Default.Star, Screen.WhaleTracker.route, feature = FeatureKey.DASHBOARD_WHALE_FEED_LIVE),
        ToolItem("BACKTESTER", "Simulate strategy", Icons.Default.Refresh, Screen.Backtester.route, targetId = TutorialTargetId.TOOLS_BACKTESTER, feature = FeatureKey.BACKTESTER)
    )

    val analysisTools = listOf(
        ToolItem("ENTRY QUALITY", "Rate your setup", Icons.Default.Search, Screen.EntryAnalysis.route, feature = FeatureKey.ENTRY_QUALITY_SCORER),
        ToolItem("RISK SCORE", "Coin risk matrix", Icons.Default.Warning, Screen.Risk.route, feature = FeatureKey.RISK_SCORING),
        ToolItem("CORRELATION", "Macro matrix", Icons.Default.Share, Screen.Correlation.route, feature = FeatureKey.CORRELATION_MATRIX),
        ToolItem("DEFI YIELDS", "TVL & Yields", Icons.Default.Info, Screen.DeFi.route, feature = FeatureKey.DEFI_YIELDS)
    )

    val advancedTools = listOf(
        ToolItem("SIGNALS", "Signal composer", Icons.Default.Add, Screen.SignalComposer.route, feature = FeatureKey.ALERTS_UNLIMITED),
        ToolItem("HALVING", "BTC Cycle analyzer", Icons.Default.DateRange, Screen.Seasonal.route, feature = FeatureKey.SEASONAL_PATTERNS),
        ToolItem("PSYCHOLOGY", "Tilt & emotion", Icons.Default.Face, Screen.Psychology.route, feature = FeatureKey.PSYCHOLOGY_LOCK),
        ToolItem("JOURNAL", "Trade history", Icons.AutoMirrored.Filled.List, Screen.Journal.route, feature = FeatureKey.TRADE_JOURNAL_UNLIMITED),
        ToolItem("PERFORMANCE", "Personal stats", Icons.Default.ThumbUp, Screen.Performance.route, feature = FeatureKey.ACHIEVEMENTS)
    )

    val quantLabTools = listOf(
        ToolItem("CYCLE SCANNER", "Fourier Cycle analysis", Icons.Default.Refresh, Screen.CycleScanner.createRoute(), feature = FeatureKey.QUANT_LAB_INDIVIDUAL),
        ToolItem("PROBABILITY", "Monte Carlo engine", Icons.Default.Info, Screen.ProbabilityEngine.createRoute(), feature = FeatureKey.QUANT_LAB_INDIVIDUAL),
        ToolItem("DYNAMISM", "Hurst & Fractal noise", Icons.Default.Search, Screen.MarketDynamism.createRoute(), feature = FeatureKey.QUANT_LAB_INDIVIDUAL),
        ToolItem("REGRESSION", "Standard deviation tunnel", Icons.Default.Build, Screen.RegressionTunnel.createRoute(), feature = FeatureKey.QUANT_LAB_INDIVIDUAL)
    )

    val adminTools = listOf(
        ToolItem("ORACLE HUB", "AI Predictions", Icons.Default.Star, Screen.Prediction.createRoute(), adminOnly = true, feature = FeatureKey.PREDICTION_ENGINES_6),
        ToolItem("CONTENT STUDIO", "AI Generator", Icons.Default.Create, Screen.ContentStudio.route, adminOnly = true, feature = FeatureKey.CONTENT_STUDIO)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp),
    ) {
        Text(
            text = ">>> TRADING_TOOLS_HUB",
            color = colors.primary,
            fontFamily = JetBrainsMono,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SectionHeader("⚙️ SYSTEM") }
            item {
                ToolGrid(listOf(
                    ToolItem("SETTINGS", "Terminal config & Login", Icons.Default.Settings, Screen.Settings.route)
                ), navController, currentTier)
            }

            item { SectionHeader("⭐ ESSENTIAL TOOLS") }
            item { ToolGrid(essentialTools, navController, currentTier) }

            item { SectionHeader("📊 ANALYSIS & DATA") }
            item { ToolGrid(analysisTools, navController, currentTier) }

            item { SectionHeader("🔬 QUANT LAB (PRO)") }
            item { ToolGrid(quantLabTools, navController, currentTier) }
            
            item { SectionHeader("⚙️ ADVANCED") }
            item { ToolGrid(advancedTools, navController, currentTier) }

            if (currentTier == AccessTier.ADMIN) {
                item { SectionHeader("🛠️ ADMIN_ONLY") }
                item { ToolGrid(adminTools, navController, currentTier) }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ToolGrid(tools: List<ToolItem>, navController: NavController, tier: AccessTier) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tools.chunked(2).forEach { rowTools ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowTools.forEach { tool ->
                    val hasAccess = tool.feature?.let { 
                        val allowed = tier.canAccess(it.requiredTier)
                        android.util.Log.d("ToolsHub", "Checking ${tool.name}: tier=${tier.name}, required=${it.requiredTier.name}, hasAccess=$allowed")
                        allowed
                    } ?: true
                    
                    ToolCard(
                        tool = tool, 
                        modifier = Modifier.weight(1f),
                        isLocked = !hasAccess,
                    ) { 
                        if (hasAccess) {
                            navController.navigate(tool.route) 
                        } else {
                            navController.navigateToPaywall(tool.feature.name.lowercase(), tool.feature)
                        }
                    }
                }
                if (rowTools.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    val colors = LocalTerminalColors.current
    Text(
        text = title,
        color = colors.amber,
        fontFamily = JetBrainsMono,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun ToolCard(
    tool: ToolItem,
    modifier: Modifier = Modifier,
    isLocked: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = LocalTerminalColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp) // Fixed height for uniform grid (Task 2)
            .border(0.5.dp, if (isLocked) colors.grid.copy(alpha = 0.5f) else colors.grid)
            .background(if (isLocked) colors.background.copy(alpha = 0.5f) else colors.background)
            .let { if (tool.targetId != null) it.tutorialTarget(tool.targetId) else it }
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isLocked) "🔒 ${tool.name}" else "[${tool.name}]",
                    color = if (isLocked) colors.dimText else colors.primary,
                    fontSize = 11.sp, // Slightly smaller to prevent crowding
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false) // Allow text to take only needed space
                )
                
                tool.feature?.let {
                    Spacer(Modifier.width(4.dp))
                    FeatureHelpIcon(
                        feature = it,
                        iconSize = 14.dp, // Adjusted size
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = tool.description,
                color = if (isLocked) colors.dimText.copy(alpha = 0.5f) else colors.dimText,
                fontSize = 10.sp,
                fontFamily = JetBrainsMono,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 2,
                lineHeight = 12.sp,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

data class ToolItem(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val route: String,
    val adminOnly: Boolean = false,
    val proOnly: Boolean = false,
    val targetId: TutorialTargetId? = null,
    val feature: FeatureKey? = null,
)
