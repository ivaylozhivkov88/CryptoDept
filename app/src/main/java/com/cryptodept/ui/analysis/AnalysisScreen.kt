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
    predictionViewModel: PredictionViewModel = hiltViewModel(),
) {
    val colors = LocalTerminalColors.current
    val context = LocalContext.current
    val state by viewModel.analysisState.collectAsStateWithLifecycle()
    val trackedCoins by viewModel.trackedCoins.collectAsStateWithLifecycle()
    val aiReport by viewModel.aiReport.collectAsStateWithLifecycle()
    val predictionState by predictionViewModel.uiState.collectAsStateWithLifecycle()

    var showHelp by remember { mutableStateOf(false) }
    var showBreakdown by remember { mutableStateOf(false) }

    val billingViewModel: com.cryptodept.viewmodel.BillingViewModel = hiltViewModel()
    val isPro by billingViewModel.billingManager.isPro.collectAsStateWithLifecycle()
    var showPaywall by remember { mutableStateOf(false) }

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
                    Text(
                        text = aiReport!!,
                        color = colors.textPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
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

                AssetSelector(assets = trackedCoins, selectedAsset = coinId, onSelect = { viewModel.loadAnalysis(it) })

                when (val uiState = state) {
                    is AnalysisUiState.Loading -> {
                        com.cryptodept.ui.components.skeletons
                            .AnalysisSkeleton()
                    }
                    is AnalysisUiState.Success -> {
                        AnalysisContentV2(
                            state = uiState,
                            isPro = isPro,
                            onRunDeepScan = { id -> predictionViewModel.startDeepAnalysis(id) },
                            onShowPaywall = { showPaywall = true },
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

        // --- OVERLAYS (Cover entire screen) ---

        if (showPaywall) {
            com.cryptodept.ui.paywall
                .PaywallScreen(onDismiss = { showPaywall = false })
        }

        when (val pState = predictionState) {
            is com.cryptodept.ui.prediction.AnalysisUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f))) {
                    com.cryptodept.ui.prediction
                        .AnalysisLoadingScreen(pState)
                }
            }
            is com.cryptodept.ui.prediction.AnalysisUiState.Success -> {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    com.cryptodept.ui.prediction.DeepAnalysisResultScreen(
                        prediction = pState.prediction,
                        modelVotes = pState.prediction.ensembleConsensus.modelVotes,
                        onDismiss = { predictionViewModel.reset() },
                    )
                }
                BackHandler { predictionViewModel.reset() }
            }
            is com.cryptodept.ui.prediction.AnalysisUiState.Error -> {
                AlertDialog(
                    onDismissRequest = { predictionViewModel.reset() },
                    containerColor = colors.background,
                    modifier = Modifier.border(1.dp, colors.danger, RectangleShape),
                    title = { Text(">>> SYSTEM ERROR", color = colors.danger, fontFamily = FontFamily.Monospace) },
                    text = { Text(pState.message, color = colors.primary, fontFamily = FontFamily.Monospace) },
                    confirmButton = { TextButton(onClick = { predictionViewModel.reset() }) { Text("DISMISS", color = colors.primary) } },
                )
            }
            else -> {}
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
) {
    val colors = LocalTerminalColors.current
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 16.dp),
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
    isPro: Boolean,
    onRunDeepScan: (String) -> Unit,
    onShowPaywall: () -> Unit,
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

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(
                    2.dp,
                    signalColor,
                    RectangleShape,
                ).background(signalColor.copy(alpha = 0.1f))
                .padding(16.dp)
                .semantics {
                    contentDescription = "Asset signal is ${signal.strength.name}"
                },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = signal.strength.name.replace("_", " "),
                color = signalColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "CONFIDENCE: ${String.format(Locale.US, "%.0f", signal.confidence * 100)}%",
                    color = signalColor.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "[WHY?]",
                    color = colors.primary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onShowBreakdown() },
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(">>> INDICATOR_MATRIX", color = colors.dimText, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
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

    OutlinedButton(
        onClick = {
            if (isPro) {
                val normalizedId =
                    when (result.coinId.lowercase()) {
                        "btc" -> "bitcoin"
                        "eth" -> "ethereum"
                        "xrp" -> "ripple"
                        "sol" -> "solana"
                        "ada" -> "cardano"
                        "dot" -> "polkadot"
                        "ltc" -> "litecoin"
                        "link" -> "chainlink"
                        "matic" -> "matic-network"
                        "avax" -> "avalanche-2"
                        "trx" -> "tron"
                        "xlm" -> "stellar"
                        "atom" -> "cosmos"
                        "shib" -> "shiba-inu"
                        "doge" -> "dogecoin"
                        else -> result.coinId.lowercase()
                    }
                onRunDeepScan(normalizedId)
            } else {
                onShowPaywall()
            }
        },
        modifier = Modifier.fillMaxWidth().height(56.dp),
        border = BorderStroke(1.dp, colors.primary),
        shape = RectangleShape,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
    ) {
        Text("> RUN_DEEP_QUANT_SCAN", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
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
                Text("VERDICT:", color = colors.dimText, fontSize = 10.sp)
                Text(sentiment.verdict.name, color = verdictColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                Text("BULL: ${sentiment.bullishPercent}%", color = colors.primary, fontSize = 9.sp)
                Text("NEUT: ${sentiment.neutralPercent}%", color = colors.amber, fontSize = 9.sp)
                Text("BEAR: ${sentiment.bearishPercent}%", color = colors.danger, fontSize = 9.sp)
            }

            Text(
                "DATA_POINTS_ANALYZED: ${sentiment.totalAnalyzed}",
                color = colors.grid,
                fontSize = 8.sp,
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
