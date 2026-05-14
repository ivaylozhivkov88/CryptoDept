package com.cryptodept.ui.prediction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.domain.model.*
import com.cryptodept.ui.theme.LocalTerminalColors
import java.util.Locale

@Composable
fun MTFSummaryTable(mtf: MTFConsensus) {
    val colors = LocalTerminalColors.current
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, colors.primary.copy(alpha = 0.2f))
                .padding(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("PERIOD", color = colors.dimText, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
            Text("TREND", color = colors.dimText, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.5f))
            Text("RSI", color = colors.dimText, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
            Text(
                "SIGNAL",
                color = colors.dimText,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1.5f),
                textAlign = androidx.compose.ui.text.style.TextAlign.End,
            )
        }

        mtf.timeframes.forEach { tf ->
            val itemColor =
                when {
                    tf.overallSignal.name.contains("BUY") -> colors.primary
                    tf.overallSignal.name.contains("SELL") -> colors.error
                    else -> colors.amber
                }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(tf.timeframe, color = colors.textPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                Text(
                    tf.trend.name,
                    color = itemColor.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1.5f),
                )
                Text(
                    String.format(Locale.US, "%.0f", tf.rsi),
                    color = colors.textPrimary.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    tf.overallSignal.name.take(6),
                    color = itemColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1.5f).padding(end = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
            }
        }
    }
}

@Composable
fun ExpandableModelRow(
    modelName: String,
    vote: ModelVote,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = LocalTerminalColors.current

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = if (expanded) "[-] $modelName" else "[+] $modelName",
                color = if (expanded) colors.primary else colors.textPrimary.copy(alpha = 0.8f),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
            )

            val dirColor =
                when {
                    vote.direction.name.contains("UP") -> colors.primary
                    vote.direction.name.contains("DOWN") -> colors.error
                    else -> colors.amber // Sideways
                }

            Text(
                text = vote.direction.name,
                color = dirColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 12.dp)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)) {
                Text(
                    "CONFIDENCE: ${(vote.confidence * 100).toInt()}%",
                    color = colors.dimText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
                Text("ENSEMBLE_WEIGHT: ${vote.weight}", color = colors.dimText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Text(
                    text = "REASONING: ${vote.reasoning}",
                    color = colors.primary.copy(0.6f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 14.sp,
                )
            }
        }
    }
}

@Composable
fun ConsensusHeader(prediction: PricePrediction) {
    val colors = LocalTerminalColors.current
    val direction = prediction.ensembleConsensus.direction
    val color =
        when {
            direction.name.contains("UP") -> colors.primary
            direction.name.contains("DOWN") -> colors.error
            else -> colors.amber
        }

    val verdict =
        when (direction) {
            Direction.STRONG_UP -> "Oracle detects high-conviction breakout cluster. Order-flow and fractal harmonics are synchronized for immediate upward expansion."
            Direction.UP -> "Bullish drift established. Intelligence engines confirm accumulation behavior and weakening overhead resistance."
            Direction.DOWN -> "Bearish pressure intensifying. Liquidity gravity is pulling the price towards historical support zones."
            Direction.STRONG_DOWN -> "Critical systemic rejection. Collective models identify high probability of an accelerated downward cascade."
            Direction.SIDEWAYS -> "Strategic equilibrium. Market participants are in a state of high disagreement. Recommended action: OBSERVE."
        }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(2.dp, color, RectangleShape)
                .background(color.copy(alpha = 0.08f))
                .padding(24.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            // "Digital Scanner" style title
            Text(
                text = ">>> ORACLE_SYNTHESIS_V4 <<<", 
                color = color.copy(0.7f), 
                fontSize = 11.sp, 
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = direction.name, 
                color = color, 
                fontSize = 32.sp, 
                fontWeight = FontWeight.Black, 
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = verdict,
                color = colors.textPrimary,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Accuracy score pill
            Surface(
                color = color.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.dp, color),
                shape = RectangleShape
            ) {
                Text(
                    text = " CONVICTION: ${(prediction.ensembleConsensus.overallConfidence * 100).toInt()}% ",
                    color = color,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ProbabilityScale(prediction: PricePrediction) {
    val colors = LocalTerminalColors.current
    val dist = prediction.priceDistribution
    val current = prediction.currentPrice
    val range = dist.percentile90 - dist.percentile10
    val currentPos = if (range != 0.0) ((current - dist.percentile10) / range).coerceIn(0.0, 1.0).toFloat() else 0.5f

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(24.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.grid)
                    .align(Alignment.Center),
            )
            Box(Modifier.fillMaxWidth(currentPos).fillMaxHeight().align(Alignment.CenterStart)) {
                Box(
                    Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(colors.textPrimary)
                        .align(Alignment.CenterEnd),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("FLOOR_ZONE", color = colors.dimText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text(
                    "$${String.format(Locale.US, "%.2f", dist.percentile10)}",
                    color = colors.error,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("MEDIAN_PRICE", color = colors.dimText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text("$${String.format(Locale.US, "%.2f", dist.percentile50)}", color = colors.textPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("QUANT_TARGET", color = colors.dimText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text(
                    "$${String.format(Locale.US, "%.2f", dist.percentile90)}",
                    color = colors.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

@Composable
fun DataQualityFooter(prediction: PricePrediction) {
    val colors = LocalTerminalColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        HorizontalDivider(color = colors.primary.copy(alpha = 0.2f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "PREDICTABILITY_SCORE: ${(prediction.dataQuality * 100).toInt()}%",
            color = colors.dimText,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            "AGREEMENT_BETWEEN_MODELS: ${(prediction.modelsAgreement * 100).toInt()}%",
            color = colors.dimText,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
fun LiquidityInsightPanel(insight: LiquidityInsight) {
    val colors = LocalTerminalColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.grid)
            .padding(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("OPEN_INTEREST", color = colors.dimText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text(
                    text = "$${String.format(Locale.US, "%.1f", insight.openInterest / 1_000_000)}M",
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${if (insight.openInterestChange24h >= 0) "+" else ""}${String.format(Locale.US, "%.1f", insight.openInterestChange24h)}% (24H)",
                    color = if (insight.openInterestChange24h >= 0) colors.primary else colors.error,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("FUNDING_RATE", color = colors.dimText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                Text(
                    text = "${String.format(Locale.US, "%.4f", insight.fundingRate)}%",
                    color = if (insight.fundingRate > 0) colors.primary else colors.error,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = insight.sentimentBias,
                    color = colors.amber,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = colors.grid.copy(alpha = 0.3f))
        Spacer(Modifier.height(8.dp))
        
        Text("LONG/SHORT RATIO (RETAIL):", color = colors.dimText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(colors.surface)
        ) {
            Box(Modifier.weight(insight.longShortRatio.toFloat().coerceAtLeast(0.01f)).fillMaxHeight().background(colors.primary))
            Box(Modifier.weight((1 - insight.longShortRatio).toFloat().coerceAtLeast(0.01f)).fillMaxHeight().background(colors.error))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${(insight.longShortRatio * 100).toInt()}% L", color = colors.primary, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            Text("${((1 - insight.longShortRatio) * 100).toInt()}% S", color = colors.error, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun EvidenceChainPanel(chain: List<EvidenceStep>) {
    val colors = LocalTerminalColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.primary.copy(alpha = 0.3f))
            .padding(12.dp)
    ) {
        chain.forEachIndexed { index, step ->
            val impactColor = when (step.impact) {
                Direction.UP, Direction.STRONG_UP -> colors.primary
                Direction.DOWN, Direction.STRONG_DOWN -> colors.error
                else -> colors.amber
            }
            
            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                Column(modifier = Modifier.weight(0.1f)) {
                    Text(
                        text = (index + 1).toString().padStart(2, '0'),
                        color = impactColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Column(modifier = Modifier.weight(0.9f)) {
                    Text(
                        text = ">>> ${step.title}",
                        color = impactColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = step.description,
                        color = colors.textPrimary.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 14.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "IMPACT: ${step.impact.name}",
                            color = impactColor.copy(alpha = 0.6f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "CONFIDENCE: ${(step.confidence * 100).toInt()}%",
                            color = colors.dimText,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
            if (index < chain.size - 1) {
                HorizontalDivider(color = colors.grid.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
fun OracleConsensusMap(votes: Map<PredictionModel, ModelVote>) {
    val colors = LocalTerminalColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.grid)
            .padding(12.dp)
    ) {
        Text("MODEL_VOTING_MAP:", color = colors.dimText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            votes.forEach { (model, vote) ->
                val color = when {
                    vote.direction.name.contains("UP") -> colors.primary
                    vote.direction.name.contains("DOWN") -> colors.error
                    else -> colors.amber
                }
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .background(color.copy(alpha = vote.confidence.coerceIn(0.2f, 1f)))
                        .border(0.5.dp, color),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = model.name.take(1),
                        color = colors.background,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
        
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("L: LINEAR", color = colors.dimText, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
            Text("F: FOURIER", color = colors.dimText, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
            Text("M: MONTE", color = colors.dimText, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
            Text("E: ELLIOTT", color = colors.dimText, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
        }
    }
}
