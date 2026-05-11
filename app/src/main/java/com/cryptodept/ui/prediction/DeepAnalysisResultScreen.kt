package com.cryptodept.ui.prediction

import android.content.ClipData
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
import com.cryptodept.domain.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DeepAnalysisResultScreen(
    prediction: PricePrediction,
    modelVotes: Map<PredictionModel, ModelVote>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showCopyToast by remember { mutableStateOf(false) }

    val preferencesService = remember { com.cryptodept.data.datastore.PreferencesService(context, com.cryptodept.util.SecurePrefsService(context)) }
    val isAdmin by preferencesService.isAdmin.collectAsState(initial = false)
    val marketingAgent = remember { MarketingStrategist() }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f)),
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
                        text = "DEEP_QUANT_ANALYSIS_V2.1",
                        color = Color(0xFF00FF41),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }

            // CONSENSUS HEADER
            item {
                ConsensusHeader(prediction)
            }

            // PRICE DISTRIBUTION
            item {
                Text(
                    text = ">>> PRICE_PROBABILITY_DISTRIBUTION (24H)",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 8.dp),
                )
                ProbabilityScale(prediction)
            }

            // MTF TABLE
            prediction.mtfConsensus?.let { mtf ->
                item {
                    Text(
                        text = ">>> MULTI_TIMEFRAME_CONFLUENCE",
                        color = Color.White.copy(alpha = 0.6f),
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
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                )
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.DarkGray)
                            .padding(horizontal = 12.dp),
                ) {
                    modelVotes.forEach { (model, vote) ->
                        ExpandableModelRow(model.displayName, vote)
                        if (model != modelVotes.keys.last()) {
                            HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                DataQualityFooter(prediction)
                Spacer(modifier = Modifier.height(16.dp))
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
                    containerColor = Color(0xFF00FF41),
                    contentColor = Color.Black,
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
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text("AI", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
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
    Box(
        modifier =
            modifier
                .background(Color(0xFF00FF41), RoundedCornerShape(4.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(message, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
