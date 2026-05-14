package com.cryptodept.ui.prediction

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    val preferencesService = remember { com.cryptodept.data.datastore.PreferencesService(context, com.cryptodept.util.SecurePrefsService(context)) }
    val isAdmin by preferencesService.isAdmin.collectAsState(initial = false)
    val marketingAgent = remember { MarketingStrategist() }
    val predictionViewModel: PredictionViewModel = hiltViewModel()

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
                        ExpandableModelRow(model.displayName, vote)
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

        // BOTTOM ACTION BUTTONS
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (isAdmin) {
                // VIRAL FACEBOOK REPORT BUTTON
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            val packageReport = marketingAgent.generateMarketingPackage(prediction)
                            val fbPost = packageReport.details["facebook_post"] ?: ""
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, fbPost)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Post Viral Report"))
                        }
                    },
                    modifier = Modifier.size(56.dp),
                    containerColor = colors.primary,
                    contentColor = colors.background,
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text("FB", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                // META.AI VIDEO PROMPT BUTTON
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            val packageReport = marketingAgent.generateMarketingPackage(prediction)
                            val videoPrompt = packageReport.details["video_prompt"] ?: ""
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, videoPrompt)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Meta.ai Video Prompt"))
                        }
                    },
                    modifier = Modifier.size(56.dp),
                    containerColor = colors.textPrimary,
                    contentColor = colors.background,
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text("AI", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            // PUBLIC SHARE BUTTON (Visible to everyone)
            FloatingActionButton(
                onClick = {
                    val shareText = predictionViewModel.generateShareText(prediction)
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, shareText)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Quant Report"))
                },
                modifier = Modifier.size(56.dp),
                containerColor = colors.primary.copy(alpha = 0.8f),
                contentColor = colors.background,
                shape = RoundedCornerShape(4.dp),
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(24.dp))
            }
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
