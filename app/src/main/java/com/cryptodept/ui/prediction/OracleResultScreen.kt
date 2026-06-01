package com.cryptodept.ui.prediction

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.domain.model.*
import com.cryptodept.ui.theme.LocalTerminalColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OracleResultScreen(
    prediction: PricePrediction,
    modelVotes: Map<PredictionModel, ModelVote>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val colors = LocalTerminalColors.current
    val coroutineScope = rememberCoroutineScope()
    var showCopyToast by remember { mutableStateOf(false) }
    var showImgOptions by remember { mutableStateOf(false) }

    val preferencesService = remember { com.cryptodept.data.datastore.PreferencesService(context, com.cryptodept.util.SecurePrefsService(context)) }
    val isAdmin by preferencesService.isAdmin.collectAsState(initial = false)
    val billingViewModel: com.cryptodept.viewmodel.BillingViewModel = hiltViewModel()
    val isPro by billingViewModel.billingManager.isPro.collectAsState()
    
    val marketingAgent = remember { MarketingStrategist() }
    val predictionViewModel: PredictionViewModel = hiltViewModel()
    val aiReport by predictionViewModel.aiReport.collectAsState()
    val isAiStreaming by predictionViewModel.isAiStreaming.collectAsState()

    val aiReportState = aiReport
    if (aiReportState != null) {
        AlertDialog(
            onDismissRequest = { predictionViewModel.dismissAiReport() },
            containerColor = colors.background,
            modifier = Modifier.border(1.dp, colors.primary, RectangleShape),
            title = {
                Text(
                    ">>> ELITE_OPERATOR_REPORT",
                    color = colors.primary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp, // EVEN SMALLER (Task 3.1)
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
                    val cleanedReport = aiReportState
                        .replace("IDENTITY: Lead Quantitative Strategist for CryptoDept Elite Terminal.", "")
                        .replace("TASK: Generate a high-impact, professional narrative report for", "")
                        .replace(Regex("(?s)REPORT_CONSTRAINTS:.*"), "")
                        .replace(Regex("(?s)INSTRUCTIONS:.*"), "")
                        .trim()

                    com.cryptodept.ui.components.StreamingText(
                        text = cleanedReport,
                        isStreaming = isAiStreaming,
                        textColor = colors.textPrimary,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, aiReportState)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Analysis Report"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("SHARE", color = colors.primary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("FB_REPORT", aiReportState)
                            clipboard.setPrimaryClip(clip)
                            showCopyToast = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("COPY FOR FB", color = colors.primary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { predictionViewModel.dismissAiReport() }) {
                    Text("CLOSE", color = colors.dimText, fontFamily = FontFamily.Monospace)
                }
            },
        )
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background.copy(alpha = 0.95f)),
    ) {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = ">>> THE_ORACLE_PROTOCOL_V4.0",
                        color = colors.primary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textPrimary)
                    }
                }
            }

            // CONSENSUS HEADER
            item {
                ConsensusHeader(prediction)
            }

            // PREDICTION CHART (PHASE Z3 - NEW)
            item {
                val historicalData by predictionViewModel.historicalData.collectAsState()
                if (historicalData.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = ">>> QUANT_FORECAST_ENGINE_V4",
                            color = colors.dimText,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                        IconButton(
                            onClick = {
                                val url = predictionViewModel.generateInfographicUrl(prediction)
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, ">>> CRYPTODEPT_ALPHA_CHART\n$url")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Chart"))
                            },
                            modifier = Modifier.size(16.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = colors.primary, modifier = Modifier.size(12.dp))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .border(1.dp, colors.grid)
                            .padding(12.dp)
                    ) {
                        Column {
                            Box(modifier = Modifier.weight(1f)) {
                                com.cryptodept.ui.components.PredictionChart(
                                    historicalData = historicalData,
                                    prediction = prediction
                                )
                            }
                            // Legend/Descriptions under the chart
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(8.dp, 2.dp).background(colors.primary))
                                    Spacer(Modifier.width(4.dp))
                                    Text("PAST_PERFORMANCE", color = colors.dimText, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(8.dp, 2.dp).background(colors.amber))
                                    Spacer(Modifier.width(4.dp))
                                    Text("QUANT_FORECAST", color = colors.amber, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }

            // EXPLAINABILITY PANEL (FEATURE 16)
            if (prediction.factors.isNotEmpty()) {
                item {
                    com.cryptodept.ui.components.PredictionExplainPanel(
                        factors = prediction.factors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // NEW: Confidence breakdown (PHASE Y2)
            item {
                val confidenceMetrics by predictionViewModel.confidenceMetrics.collectAsState()
                confidenceMetrics?.let { metrics ->
                    com.cryptodept.ui.components.ConfidenceMetricsCard(
                        metrics = metrics,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // CONSENSUS MAP (PHASE X)
            item {
                OracleConsensusMap(prediction.ensembleConsensus.modelVotes)
            }

            // EVIDENCE CHAIN (PHASE X)
            if (prediction.evidenceChain.isNotEmpty()) {
                item {
                    Text(
                        text = ">>> CHAIN_OF_EVIDENCE",
                        color = colors.dimText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    EvidenceChainPanel(prediction.evidenceChain)
                }
            }

            // PRICE DISTRIBUTION
            item {
                Text(
                    text = ">>> PRICE_PROBABILITY_DISTRIBUTION (24H)",
                    color = colors.dimText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 8.dp),
                )
                ProbabilityScale(prediction)
            }

            // LIQUIDITY INSIGHTS (PHASE X)
            prediction.liquidityInsight?.let { insight ->
                item {
                    Text(
                        text = ">>> LIQUIDITY_&_ORDERFLOW_INSIGHTS",
                        color = colors.dimText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    LiquidityInsightPanel(insight)
                }
            }

            // MTF TABLE
            prediction.mtfConsensus?.let { mtf ->
                item {
                    Text(
                        text = ">>> MULTI_TIMEFRAME_CONFLUENCE",
                        color = colors.dimText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    MTFSummaryTable(mtf)
                }
            }

            // ENSEMBLE BREAKDOWN
            item {
                Text(
                    text = ">>> ENSEMBLE_MODEL_VOTES",
                    color = colors.dimText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                )
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.grid)
                            .padding(horizontal = 12.dp),
                ) {
                    modelVotes.forEach { (model, vote) ->
                        ExpandableModelRow(
                            modelName = model.displayName, 
                            vote = vote,
                            coinId = prediction.coinId
                        )
                        if (model != modelVotes.keys.last()) {
                            HorizontalDivider(color = colors.grid, thickness = 0.5.dp)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                DataQualityFooter(prediction)
                Spacer(modifier = Modifier.height(100.dp)) // Ensuring FABs don't cover content
            }
        }

        // BOTTOM ACTION BUTTONS - PHASE O CONTENT ORCHESTRATOR
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (isAdmin || isPro) {
                // 1. NARRATIVE REPORT (TEXT)
                FloatingActionButton(
                    onClick = {
                        predictionViewModel.generateAIReport(prediction)
                    },
                    modifier = Modifier.size(56.dp),
                    containerColor = colors.amber,
                    contentColor = colors.background,
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text("N", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                // 2. VIDEO PROMPT (VID)
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            val packageReport = marketingAgent.generateMarketingPackage(prediction)
                            val videoPrompt = packageReport.details["video_prompt"] ?: ""
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, videoPrompt)
                                type = "text/plain"
                                // Subject hint for Meta AI / Reels
                                putExtra(Intent.EXTRA_SUBJECT, "CryptoDept Video Script for Meta AI")
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Send to Meta AI / Reels"))
                        }
                    },
                    modifier = Modifier.size(56.dp),
                    containerColor = colors.textPrimary,
                    contentColor = colors.background,
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text("VID", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }

                // 3. INFOGRAPHIC CHART (IMG) - NEW
                FloatingActionButton(
                    onClick = { showImgOptions = true },
                    modifier = Modifier.size(56.dp),
                    containerColor = colors.primary,
                    contentColor = colors.background,
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text("IMG", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }
        }

        if (showImgOptions) {
        AlertDialog(
            onDismissRequest = { showImgOptions = false },
            containerColor = colors.background,
            modifier = Modifier.border(1.dp, colors.primary, RectangleShape),
            title = { Text("SELECT_OUTPUT_METHOD", color = colors.primary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Select how you want to generate or share the infographic visual.", color = colors.dimText, fontSize = 12.sp)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            showImgOptions = false
                            val url = predictionViewModel.generateInfographicUrl(prediction)
                            val shareText = buildString {
                                append("📊 CRYPTODEPT QUANT INFOGRAPHIC — ${prediction.coinId.uppercase()}\n")
                                append("VIEW CHART: $url\n\n")
                                append(">>> ANALYSIS_SUMMARY: Market showing ${prediction.ensembleConsensus.direction.name.lowercase()} bias with ${(prediction.ensembleConsensus.overallConfidence * 100).toInt()}% conviction.")
                            }
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Chart Link"))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RectangleShape
                    ) {
                        Text("SHARE QUICKCHART LINK", color = colors.background, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            showImgOptions = false
                            val aiPrompt = predictionViewModel.generateImagePrompt(prediction)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = android.content.ClipData.newPlainText("AI_PROMPT", aiPrompt)
                            clipboard.setPrimaryClip(clip)
                            showCopyToast = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.amber),
                        shape = RectangleShape
                    ) {
                        Text("COPY AI IMAGE PROMPT", color = colors.background, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showCopyToast) {
            Toast(
                message = "REPORT COPIED TO CLIPBOARD",
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp),
            )
            LaunchedEffect(Unit) {
                delay(2000)
                showCopyToast = false
            }
        }
    }
}

@Composable
fun Toast(
    message: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTerminalColors.current
    Box(
        modifier =
            modifier
                .background(colors.primary, RoundedCornerShape(4.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(message, color = colors.background, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
