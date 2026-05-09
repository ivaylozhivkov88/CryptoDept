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
import com.cryptodept.domain.model.*
import kotlinx.coroutines.delay

@Composable
fun DeepAnalysisResultScreen(
    prediction: PricePrediction,
    modelVotes: Map<PredictionModel, ModelVote>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var showCopyToast by remember { mutableStateOf(false) }

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
            // SHARE TEXT BUTTON
            FloatingActionButton(
                onClick = {
                    val shareText = generateShareText(prediction)
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Analysis Report", shareText)
                    clipboard.setPrimaryClip(clip)
                    showCopyToast = true
                },
                modifier = Modifier.size(56.dp),
                containerColor = Color(0xFFFFB000),
                contentColor = Color.Black,
                shape = RoundedCornerShape(4.dp),
            ) {
                Text("📄", fontSize = 20.sp)
            }

            // SHARE AI PROMPT BUTTON
            FloatingActionButton(
                onClick = {
                    val prompt = generateAiPrompt(prediction)
                    val sendIntent: Intent =
                        Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, prompt)
                            type = "text/plain"
                        }
                    val shareIntent = Intent.createChooser(sendIntent, "Share AI Visualizer Prompt")
                    context.startActivity(shareIntent)
                },
                modifier = Modifier.size(56.dp),
                containerColor = Color(0xFF00FF41),
                contentColor = Color.Black,
                shape = RoundedCornerShape(4.dp),
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share Prompt")
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
