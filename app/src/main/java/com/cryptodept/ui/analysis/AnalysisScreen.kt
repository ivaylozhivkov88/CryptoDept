package com.cryptodept.ui.analysis

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptodept.domain.model.*
import com.cryptodept.ui.tutorial.tutorialTarget
import com.cryptodept.domain.tutorial.TutorialTargetId
import com.cryptodept.ui.components.StreamingText
import com.cryptodept.ui.components.TechnicalBreakdownSheet
import com.cryptodept.ui.components.TerminalErrorOverlay
import com.cryptodept.ui.components.TerminalHelpDialog
import com.cryptodept.ui.navigation.Screen
import com.cryptodept.ui.prediction.PredictionViewModel
import com.cryptodept.ui.theme.*
import com.cryptodept.viewmodel.AnalysisUiState
import com.cryptodept.viewmodel.AnalysisViewModel
import java.util.Locale

@Composable
fun AnalysisScreen(
    coinId: String,
    navController: androidx.navigation.NavController,
    viewModel: AnalysisViewModel = hiltViewModel(),
) {
    val colors = LocalTerminalColors.current
    val context = LocalContext.current
    val state by viewModel.analysisState.collectAsStateWithLifecycle()
    val trackedCoins by viewModel.trackedCoins.collectAsStateWithLifecycle()
    val aiReport by viewModel.aiReport.collectAsStateWithLifecycle()
    val isAiStreaming by viewModel.isAiStreaming.collectAsStateWithLifecycle()

    var showHelp by remember { mutableStateOf(false) }
    var showBreakdown by remember { mutableStateOf(false) }

    LaunchedEffect(coinId) {
        viewModel.loadAnalysis(coinId)
    }

    if (showBreakdown && state is AnalysisUiState.Success) {
        val success = (state as AnalysisUiState.Success).result
        TechnicalBreakdownSheet(
            traces = success.traces,
            onDismiss = { showBreakdown = false },
        )
    }

    if (aiReport != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAiReport() },
            containerColor = colors.background,
            modifier = Modifier.border(1.dp, colors.primary, RectangleShape),
            title = {
                Text(
                    ">>> NARRATIVE AI REPORT",
                    color = colors.primary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 500.dp)
                            .verticalScroll(rememberScrollState()),
                ) {
                    StreamingText(
                        text = aiReport!!,
                        isStreaming = isAiStreaming,
                        textColor = colors.textPrimary,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    shareAnalysis(context, aiReport!!)
                }) {
                    Text("SHARE", color = colors.primary, fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissAiReport() }) {
                    Text("CLOSE", color = colors.dimText, fontFamily = FontFamily.Monospace)
                }
            },
        )
    }

    if (showHelp) {
        TerminalHelpDialog(onDismiss = { showHelp = false })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(colors.background),
        ) {
            // Scrollable Content
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                // --- HEADER ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = ">>> TERMINAL_DEPT_V3",
                        color = colors.primary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                    )

                    Row {
                        /*
                        if (state is AnalysisUiState.Success) {
                            IconButton(onClick = {
                                val text = viewModel.generateShareText(state as AnalysisUiState.Success)
                                shareAnalysis(context, text)
                            }) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = colors.primary, modifier = Modifier.size(20.dp))
                            }

                            if (isAdmin) {
                                IconButton(onClick = { viewModel.generateAIReport(state as AnalysisUiState.Success) }) {
                                    Text("POST", color = colors.primary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }

                                IconButton(onClick = { viewModel.generateVideoTeaser(state as AnalysisUiState.Success) }) {
                                    Text("VIDEO", color = colors.primary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }
                        }
                         */
                    }
                }

                HorizontalDivider(color = colors.grid, modifier = Modifier.padding(vertical = 8.dp))

                AssetSelector(
                    assets = trackedCoins, 
                    selectedAsset = coinId, 
                    onSelect = { viewModel.loadAnalysis(it) },
                    modifier = Modifier.tutorialTarget(TutorialTargetId.ANALYSIS_COIN_SELECTOR)
                )

                when (val uiState = state) {
                    is AnalysisUiState.Loading -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(top = 32.dp)
                        ) {
                            Text(
                                text = ">>> ANALYZING_SYSTEM_DATA...",
                                color = colors.primary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                                color = colors.primary,
                                trackColor = colors.grid
                            )
                            com.cryptodept.ui.components.skeletons.AnalysisSkeleton()
                        }
                    }
                    is AnalysisUiState.Success -> {
                        // MINI CHART FOR SCANNER LOOK
                        val prices = uiState.result.ohlcData.map { it.close }
                        if (prices.isNotEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().height(100.dp).padding(vertical = 8.dp)) {
                                com.cryptodept.ui.components.SimpleLineChart(
                                    data = uiState.result.ohlcData,
                                    lineColor = if (prices.last() > prices.first()) colors.primary else colors.danger
                                )
                            }
                        }

                        AnalysisContentV2(
                            state = uiState,
                            onShowBreakdown = { showBreakdown = true },
                        )
                    }
                    is AnalysisUiState.Error -> {
                        TerminalErrorOverlay(message = uiState.message, onRetry = { viewModel.loadAnalysis(coinId) })
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Pinned Bottom Bar
            Box(modifier = Modifier.padding(16.dp).imePadding()) {
                com.cryptodept.ui.components.TerminalCommandBar(onCommandEntered = { cmd ->
                    val parts = cmd.uppercase().split(" ")
                    when (parts[0]) {
                        "HELP" -> showHelp = true
                        "LOGOUT" -> viewModel.setAdminStatus(false)
                        else -> handleGlobalCommand(cmd, navController)
                    }
                })
            }
        }
    }
}

fun handleGlobalCommand(
    cmd: String,
    navController: androidx.navigation.NavController,
) {
    val parts = cmd.uppercase().split(" ")
    when (parts[0]) {
        "HELP" -> { /* Open Help */ }
        "ALERTS" -> navController.navigate("alerts")
        "NEWS" -> navController.navigate("news")
        "MATRIX" -> navController.navigate(Screen.Correlation.route)
        "SETTINGS" -> navController.navigate("settings")
        "RISK" -> navController.navigate("risk")
        "BACK" -> navController.popBackStack()
        "DASHBOARD" -> navController.navigate("dashboard")
        "CHART" -> {
            val coin = if (parts.size > 1) parts[1].lowercase() else "bitcoin"
            navController.navigate("charts/$coin")
        }
        "ANALYSIS" -> {
            val coin = if (parts.size > 1) parts[1].lowercase() else "bitcoin"
            navController.navigate("analysis?coinId=$coin")
        }
    }
}

@Composable
fun AssetSelector(
    assets: List<String>,
    selectedAsset: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTerminalColors.current
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        assets.forEach { asset ->
            val isSelected = asset.equals(selectedAsset, ignoreCase = true)
            Box(
                modifier =
                    Modifier
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) colors.primary else colors.grid,
                            shape = RectangleShape,
                        ).background(if (isSelected) colors.primary.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { onSelect(asset) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    text = asset.uppercase(),
                    color = if (isSelected) colors.primary else colors.dimText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
fun AnalysisContentV2(
    state: AnalysisUiState.Success,
    onShowBreakdown: () -> Unit,
) {
    val colors = LocalTerminalColors.current
    val result = state.result
    val signal = result.compositeSignal
    val signalColor =
        when (signal.strength) {
            SignalStrength.STRONG_BUY, SignalStrength.BUY -> colors.primary
            SignalStrength.STRONG_SELL, SignalStrength.SELL -> colors.danger
            else -> colors.amber
        }

    // NEW TECHNICAL HEADER (Scanner style, not Oracle style)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.grid)
            .background(colors.grid.copy(alpha = 0.05f))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("SCANNER_VERDICT:", color = colors.dimText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text(
                    text = signal.strength.name.replace("_", " "),
                    color = signalColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("CONFIDENCE:", color = colors.dimText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text(
                    text = "${String.format(Locale.US, "%.0f", signal.confidence * 100)}%",
                    color = signalColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = colors.grid.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "RSI: ${String.format(Locale.US, "%.2f", result.rsiValue)}",
                color = colors.textPrimary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "[ VIEW_TRACE_LOGS ]",
                color = colors.primary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onShowBreakdown() },
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(">>> INDICATOR_MATRIX", color = colors.dimText, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.tutorialTarget(TutorialTargetId.ANALYSIS_INDICATORS))
    result.compositeSignal.indicators.forEach { ind ->
        val indColor =
            when (ind.sentiment) {
                Sentiment.BULLISH -> colors.primary
                Sentiment.BEARISH -> colors.danger
                else -> colors.amber
            }
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(ind.name, color = colors.amber, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
            Text(ind.value, color = colors.primary, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
            Text(ind.sentiment.name, color = indColor, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    SentimentSection(result.sentiment)

    Spacer(modifier = Modifier.height(16.dp))

    if (result.patterns.isNotEmpty()) {
        Text(">>> PATTERN_DETECTED", color = colors.dimText, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        result.patterns.forEach { pattern ->
            Text(
                text = "[!] ${pattern.pattern.name}: ${pattern.description}",
                color = if (pattern.isBullish) colors.primary else colors.danger,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    Text(">>> FIBONACCI_RETRACEMENT", color = colors.dimText, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    result.fibonacci.forEach { (level, price) ->
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(level, color = colors.dimText, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            Text("$${String.format(Locale.US, "%.2f", price)}", color = colors.amber, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
fun SentimentSection(sentiment: com.cryptodept.domain.usecase.SentimentResult?) {
    val colors = LocalTerminalColors.current
    if (sentiment == null) return

    val verdictColor =
        when (sentiment.verdict) {
            com.cryptodept.domain.usecase.SentimentVerdict.STRONGLY_BULLISH,
            com.cryptodept.domain.usecase.SentimentVerdict.BULLISH,
            -> colors.primary
            com.cryptodept.domain.usecase.SentimentVerdict.STRONGLY_BEARISH,
            com.cryptodept.domain.usecase.SentimentVerdict.BEARISH,
            -> colors.danger
            else -> colors.amber
        }

    Text(">>> MARKET_SENTIMENT (REDDIT/CRYPTO_PANIC)", color = colors.dimText, fontSize = 12.sp, fontFamily = FontFamily.Monospace)

    Box(
        modifier = Modifier.fillMaxWidth().border(1.dp, colors.grid).padding(12.dp),
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("VERDICT:", color = colors.dimText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text(sentiment.verdict.name, color = verdictColor, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth().height(8.dp).border(0.5.dp, colors.grid)) {
                Box(
                    modifier =
                        Modifier
                            .weight(
                                sentiment.bullishPercent.toFloat().coerceAtLeast(1f),
                            ).fillMaxHeight()
                            .background(colors.primary),
                )
                Box(
                    modifier =
                        Modifier
                            .weight(
                                sentiment.neutralPercent.toFloat().coerceAtLeast(1f),
                            ).fillMaxHeight()
                            .background(colors.amber),
                )
                Box(
                    modifier =
                        Modifier
                            .weight(
                                sentiment.bearishPercent.toFloat().coerceAtLeast(1f),
                            ).fillMaxHeight()
                            .background(colors.danger),
                )
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("BULL: ${sentiment.bullishPercent}%", color = colors.primary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text("NEUT: ${sentiment.neutralPercent}%", color = colors.amber, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text("BEAR: ${sentiment.bearishPercent}%", color = colors.danger, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }

            Text(
                "DATA_POINTS_ANALYZED: ${sentiment.totalAnalyzed}",
                color = colors.grid,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

fun shareAnalysis(
    context: Context,
    report: String,
) {
    val intent =
        Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, report)
            type = "text/plain"
        }
    context.startActivity(Intent.createChooser(intent, "SEND_TERMINAL_REPORT"))
}
