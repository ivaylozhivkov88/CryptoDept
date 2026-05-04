package com.cryptodept.ui.prediction

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import java.util.Locale

@Composable
fun DeepAnalysisResultScreen(
    prediction: PricePrediction,
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    var showCopyToast by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("< CLOSE_ANALYSIS", color = Color(0xFF00FF41), modifier = Modifier.clickable { onDismiss() }, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                ConsensusHeader(prediction)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // NEW: MTF ALIGNMENT SECTION
            prediction.mtfConsensus?.let { mtf ->
                item {
                    Text(
                        text = ">>> MTF_ALIGNMENT_MATRIX:",
                        color = Color(0xFF00FF41),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    MTFSummaryTable(mtf)
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            item {
                Text(
                    text = ">>> PROBABILITY_MAP (24H_VOLATILITY):",
                    color = Color(0xFF00FF41),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                ProbabilityScale(prediction)
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                Text(
                    text = ">>> QUANT_MODEL_VOTES:",
                    color = Color(0xFF00FF41),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            val votes = prediction.ensembleConsensus.modelVotes.toList()
            items(votes) { (model, vote) ->
                ExpandableModelRow(model.name, vote)
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                DataQualityFooter(prediction)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // BOTTOM ACTION BUTTONS
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // SHARE TEXT BUTTON (NEW)
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
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("📄", fontSize = 20.sp)
            }

            // SHARE AI PROMPT BUTTON (VIDEO)
            FloatingActionButton(
                onClick = {
                    val prompt = generateAiPrompt(prediction)
                    val sendIntent: Intent = Intent().apply {
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
                shape = RoundedCornerShape(4.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share Prompt")
            }
        }

        // TOAST MESSAGE
        if (showCopyToast) {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2000)
                showCopyToast = false
            }
            Toast(
                message = "📋 ANALYSIS COPIED TO CLIPBOARD",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            )
        }
    }
}

@Composable
fun Toast(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xFF00FF41).copy(alpha = 0.9f), RoundedCornerShape(4.dp))
            .padding(12.dp)
    ) {
        Text(message, color = Color.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

private fun generateShareText(prediction: PricePrediction): String {
    return buildString {
        val coinId = prediction.coinId
        val currentPrice = String.format(Locale.US, "%.2f", prediction.currentPrice)
        val consensus = prediction.ensembleConsensus
        val consensusPercent = (consensus.overallConfidence * 100).toInt()
        val dateFormat = java.text.SimpleDateFormat("HH:mm:ss dd.MM.yyyy", Locale.US)
        val formattedDate = dateFormat.format(prediction.timestamp)

        // ЗАГЛАВИЕ
        append("════════════════════════════════════════\n")
        append("🚀 CRYPTODEPT DEEP QUANT ANALYSIS — $coinId\n")
        append("════════════════════════════════════════\n\n")

        // ТЕКУЩО СЪСТОЯНИЕ
        append(">>> CURRENT_STATE\n")
        append("PRICE: $$$currentPrice\n")
        append("TIMESTAMP: $formattedDate\n")
        append("CONSENSUS: ${consensus.direction.name.replace("_", " ")} ($consensusPercent% confidence)\n\n")

        // ВСИЧКИ МОДЕЛИ С ТЕХНИТЕ АНАЛИЗИ
        consensus.modelVotes.forEach { (model, vote) ->
            append(">>> ${model.displayName.uppercase()}\n")
            append("DIRECTION: ${vote.direction.name.replace("_", " ")}\n")
            append("TARGET: $${String.format(Locale.US, "%.2f", vote.targetPrice)}\n")
            append("CONFIDENCE: ${(vote.confidence * 100).toInt()}%\n")
            append("WEIGHT: ${(vote.weight * 100).toInt()}%\n")
            append("ANALYSIS: ${vote.reasoning}\n\n")
        }

        // AGREEMENT SCORE
        append(">>> ENSEMBLE_AGREEMENT\n")
        append("MODELS_ALIGNED: ${(consensus.agreementScore * 100).toInt()}%\n")
        if (consensus.dissenterModels.isNotEmpty()) {
            append("DISSENTER_MODELS: ${consensus.dissenterModels.joinToString(", ") { it.displayName }}\n")
        }
        append("\n")

        // THE VERDICT
        append(">>> THE_CRYPTODEPT_VERDICT\n")
        val verdict = when {
            consensusPercent >= 70 -> "🟢 STRONG ${consensus.direction.name} — Ensemble conviction is HIGH"
            consensusPercent >= 55 -> "🟡 MILD ${consensus.direction.name} — Slight edge detected"
            consensusPercent in 45..54 -> "⚪ NEUTRAL — Market equilibrium"
            else -> "🔴 STRONG ${consensus.direction.name} — Risk is elevated"
        }
        append(verdict + "\n\n")

        // PROBABILITY DISTRIBUTION
        append(">>> PRICE_DISTRIBUTION\n")
        append("10TH_PERCENTILE: $${String.format(Locale.US, "%.2f", prediction.priceDistribution.percentile10)}\n")
        append("50TH_PERCENTILE: $${String.format(Locale.US, "%.2f", prediction.priceDistribution.percentile50)}\n")
        append("90TH_PERCENTILE: $${String.format(Locale.US, "%.2f", prediction.priceDistribution.percentile90)}\n")
        append("STD_DEVIATION: $${String.format(Locale.US, "%.2f", prediction.priceDistribution.standardDeviation)}\n\n")

        // TIMEFRAME TARGETS
        append(">>> MULTI_TIMEFRAME_TARGETS\n")
        append("1H:  $${String.format(Locale.US, "%.2f", prediction.prediction1h.mid)} (${prediction.prediction1h.direction.name})\n")
        append("4H:  $${String.format(Locale.US, "%.2f", prediction.prediction4h.mid)} (${prediction.prediction4h.direction.name})\n")
        append("24H: $${String.format(Locale.US, "%.2f", prediction.prediction24h.mid)} (${prediction.prediction24h.direction.name})\n")
        append("7D:  $${String.format(Locale.US, "%.2f", prediction.prediction7d.mid)} (${prediction.prediction7d.direction.name})\n\n")

        // FOOTER
        append("════════════════════════════════════════\n")
        append("📊 Analysis: Ensemble of 7 Quantitative Models\n")
        append("⚠️  DISCLAIMER: Not financial advice. Trade at your own risk.\n")
        append("#CryptoDept #DeepQuantAnalysis #$coinId #Crypto\n")
        append("🚀 LIKE IF YOU'RE FOLLOWING THIS ANALYSIS!\n")
    }
}

private fun generateAiPrompt(prediction: PricePrediction): String {
    val asset = prediction.coinId.uppercase()
    val sentiment = prediction.ensembleConsensus.direction

    // Using descriptive names instead of special characters to avoid AI "hallucinations"
    val symbolDesc = when (asset) {
        "BTC", "BITCOIN" -> "the classic orange Bitcoin 'B' logo"
        "ETH", "ETHEREUM" -> "the blue Ethereum crystal diamond symbol"
        "XRP", "RIPPLE" -> "the modern white Ripple 'X' logo"
        "SOL", "SOLANA" -> "the Solana S-shaped logo with gradient colors"
        "ADA", "CARDANO" -> "the Cardano circular dot constellation logo"
        "DOGE", "DOGECOIN" -> "the Dogecoin golden 'D' logo"
        "DOT", "POLKADOT" -> "the Polkadot pink dot circle logo"
        "MATIC", "POLYGON" -> "the Polygon purple geometric logo"
        else -> "the futuristic digital logo for $asset"
    }


    val dateStr = java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.US).format(java.util.Date())
    val low = prediction.priceDistribution.percentile10
    val high = prediction.priceDistribution.percentile90

    val mood = when (sentiment) {
        Direction.STRONG_UP -> "aggressive bullish energy, golden light"
        Direction.UP -> "positive growth, green neon skyscraper reflections"
        Direction.DOWN -> "bearish storm, red lightning"
        Direction.STRONG_DOWN -> "market crash, red lava"
        Direction.SIDEWAYS -> "perfect stability, zen digital garden"
    }

    val verdict = when (sentiment) {
        Direction.STRONG_UP, Direction.UP -> "BULLISH BREAKOUT"
        Direction.STRONG_DOWN, Direction.DOWN -> "BEARISH DROP"
        Direction.SIDEWAYS -> "SIDEWAYS STABLE"
    }

    // Static-focused prompt to prevent text distortion and AI hallucinations
    return "COMMAND: Create a 5-second STATIC video with NO camera movement and NO text animation. " +
            "SCENE: A central, ultra-sharp high-contrast terminal screen. " +
            "ATMOSPHERE: Still environment with $mood. Only subtle background particles. " +
            "TEXT INSTRUCTION: The screen MUST remain perfectly still. Display the following text EXACTLY as written: " +
            "\"SYMBOL: $asset\" " +
            "\"VERDICT: $verdict\" " +
            "\"CURRENT: ${String.format(java.util.Locale.US, "$%.2f", prediction.currentPrice)}\" " +
            "\"LOW: ${String.format(java.util.Locale.US, "$%.2f", low)}\" " +
            "\"HIGH: ${String.format(java.util.Locale.US, "$%.2f", high)}\" " +
            "\"DATE: $dateStr\". " +
            "VISUALS: Place $symbolDesc at the top. Use professional, sharp, bold neon fonts. " +
            "All text must be in English and perfectly readable. Do not animate the letters."
}

@Composable
fun MTFSummaryTable(mtf: MTFConsensus) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF00FF41).copy(alpha = 0.2f))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("PERIOD", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
            Text("TREND", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.5f))
            Text("RSI", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
            Text("SIGNAL", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.5f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
        }

        mtf.timeframes.forEach { tf ->
            val color = when {
                tf.overallSignal.name.contains("BUY") -> Color(0xFF00FF41)
                tf.overallSignal.name.contains("SELL") -> Color.Red
                else -> Color(0xFFFFB000)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tf.timeframe, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                Text(tf.trend.name, color = color.copy(alpha = 0.8f), fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.5f))
                Text(String.format(Locale.US, "%.0f", tf.rsi), color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                Text(tf.overallSignal.name.take(6), color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.5f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            }
        }
    }
}

@Composable
fun ExpandableModelRow(modelName: String, vote: ModelVote) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = 12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = if (expanded) "[-] $modelName" else "[+] $modelName",
                color = if (expanded) Color(0xFF00FF41) else Color.White.copy(alpha = 0.8f),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )

            val dirColor = when {
                vote.direction.name.contains("UP") -> Color(0xFF00FF41)
                vote.direction.name.contains("DOWN") -> Color.Red
                else -> Color(0xFFFFB000) // Sideways
            }

            Text(
                text = vote.direction.name,
                color = dirColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)) {
                Text("CONFIDENCE: ${(vote.confidence * 100).toInt()}%", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text("ENSEMBLE_WEIGHT: ${vote.weight}", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text(
                    text = "REASONING: ${vote.reasoning}",
                    color = Color(0xFF00FF41).copy(0.6f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun ConsensusHeader(prediction: PricePrediction) {
    val direction = prediction.ensembleConsensus.direction
    val color = when {
        direction.name.contains("UP") -> Color(0xFF00FF41)
        direction.name.contains("DOWN") -> Color.Red
        else -> Color(0xFFFFB000)
    }

    val verdict = when (direction) {
        Direction.STRONG_UP -> "System detects high-probability breakout. Momentum indicators are synchronized for upward movement."
        Direction.UP -> "Bullish bias confirmed. Key resistance levels are weakening."
        Direction.DOWN -> "Bearish pressure mounting. Liquidity is shifting towards lower support zones."
        Direction.STRONG_DOWN -> "Critical breakdown imminent. Multiple models signal high risk of further decline."
        Direction.SIDEWAYS -> "Consolidation phase. Market is searching for clear direction amidst high disagreement."
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, color, RoundedCornerShape(2.dp))
            .background(color.copy(alpha = 0.05f))
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(">>> OVERALL_MARKET_SENTIMENT", color = color.copy(0.6f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text(text = direction.name, color = color, fontSize = 26.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = verdict,
                color = Color.White.copy(0.8f),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = "AGGREGATED_CONFIDENCE: ${(prediction.ensembleConsensus.overallConfidence * 100).toInt()}%", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun ProbabilityScale(prediction: PricePrediction) {
    val dist = prediction.priceDistribution
    val current = prediction.currentPrice
    val range = dist.percentile90 - dist.percentile10
    val currentPos = if (range != 0.0) ((current - dist.percentile10) / range).coerceIn(0.0, 1.0).toFloat() else 0.5f

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(24.dp)) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.DarkGray).align(Alignment.Center))
            Box(Modifier.fillMaxWidth(currentPos).fillMaxHeight().align(Alignment.CenterStart)) {
                Box(Modifier.width(2.dp).fillMaxHeight().background(Color.White).align(Alignment.CenterEnd))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("FLOOR_ZONE", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text("$${String.format(Locale.US, "%.2f", dist.percentile10)}", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("MEDIAN_PRICE", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text("$${String.format(Locale.US, "%.2f", dist.percentile50)}", color = Color.White, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("QUANT_TARGET", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text("$${String.format(Locale.US, "%.2f", dist.percentile90)}", color = Color(0xFF00FF41), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DataQualityFooter(prediction: PricePrediction) {
    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        HorizontalDivider(color = Color(0xFF00FF41).copy(alpha = 0.2f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(12.dp))
        Text("PREDICTABILITY_SCORE: ${(prediction.dataQuality * 100).toInt()}%", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        Text("AGREEMENT_BETWEEN_MODELS: ${(prediction.modelsAgreement * 100).toInt()}%", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}