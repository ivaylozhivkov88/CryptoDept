package com.cryptodept.ui.prediction

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.domain.model.*

// Terminal Colors
private val TermGreen = Color(0xFF00FF41)
private val TermBg = Color(0xFF0A0A0A)
private val TermAmber = Color(0xFFFFB000)
private val TermRed = Color(0xFFFF3131)

@Composable
fun PredictionScreen(prediction: PricePrediction) {
    Surface(color = TermBg, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier =
                Modifier
                    .padding(8.dp)
                    .border(1.dp, TermGreen, RoundedCornerShape(4.dp))
                    .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { TerminalHeader(prediction) }
            item { ConsensusSection(prediction.ensembleConsensus) }
            item { SectionHeader("PRICE TARGETS") }
            item { PriceTargetsSection(prediction) }
            item { SectionHeader("PROBABILITY DISTRIBUTION") }
            item { DistributionSection(prediction.priceDistribution) }
            item { SectionHeader("MODEL BREAKDOWN") }
            items(prediction.ensembleConsensus.modelVotes.toList()) { (model, vote) ->
                ModelRow(model, vote, prediction.ensembleConsensus.dissenterModels.contains(model))
            }
            item { Footer() }
        }
    }
}

@Composable
fun TerminalHeader(p: PricePrediction) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(">>> PREDICTION ENGINE v2.0", color = TermGreen, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
            Text("[${p.coinId} ▼]", color = TermGreen, fontWeight = FontWeight.Bold)
        }
        Text(
            "DATA QUALITY: ${"█".repeat(
                (p.dataQuality * 10).toInt(),
            )}${"░".repeat(10 - (p.dataQuality * 10).toInt())} ${(p.dataQuality * 100).toInt()}%",
            color = TermGreen,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
        )
        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = TermGreen, thickness = 1.dp)
    }
}

@Composable
fun ConsensusSection(consensus: EnsembleConsensus) {
    val color =
        when (consensus.direction) {
            Direction.STRONG_UP, Direction.UP -> TermGreen
            Direction.DOWN, Direction.STRONG_DOWN -> TermRed
            else -> TermAmber
        }
    Column {
        Text("ENSEMBLE CONSENSUS: $color ${consensus.direction}", color = color, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Confidence: ████████░░ ${(consensus.overallConfidence * 100).toInt()}%", color = TermGreen)
        Text("Agreement:  ${(consensus.agreementScore * 10).toInt()}/10 MODELS", color = TermGreen)
    }
}

@Composable
fun PriceTargetsSection(p: PricePrediction) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TargetLine("1H", p.prediction1h, p.currentPrice)
        TargetLine("4H", p.prediction4h, p.currentPrice)
        TargetLine("24H", p.prediction24h, p.currentPrice)
        TargetLine("7D", p.prediction7d, p.currentPrice)
    }
}

@Composable
fun TargetLine(
    label: String,
    target: PriceTarget,
    current: Double,
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$label L: ${target.low.toInt()}", color = TermGreen, fontSize = 10.sp)
            Text("M: ${target.mid.toInt()}", color = TermGreen, fontSize = 10.sp)
            Text("H: ${target.high.toInt()}", color = TermGreen, fontSize = 10.sp)
        }
        Canvas(Modifier.fillMaxWidth().height(12.dp)) {
            drawLine(color = TermGreen, start = Offset(0f, size.height / 2), end = Offset(size.width, size.height / 2), strokeWidth = 2f)
            val pos = ((current - target.low) / (target.high - target.low)).coerceIn(0.0, 1.0).toFloat()
            drawCircle(color = TermGreen, radius = 6f, center = Offset(size.width * pos, size.height / 2))
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text("╠════ $title ════════", color = TermGreen, fontSize = 12.sp)
}

@Composable
fun ModelRow(
    model: PredictionModel,
    vote: ModelVote,
    isDissenter: Boolean,
) {
    val color = if (isDissenter) TermAmber else TermGreen
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("${if (isDissenter) "⚠ " else ""}${model.displayName.take(15)}", color = color, fontSize = 12.sp)
        Text("${vote.direction} | ${vote.targetPrice.toInt()}", color = color, fontSize = 12.sp)
    }
}

@Composable
fun DistributionSection(dist: PriceDistribution) {
    Column {
        DistributionBar("50%ile (MED)", dist.percentile50, dist.percentile90)
        Text(
            "SKEW: ${if (dist.skewness > 0) "▲ BULLISH" else "▼ BEARISH"} (${String.format("%.2f", dist.skewness)})",
            color = TermGreen,
            fontSize = 11.sp,
        )
    }
}

@Composable
fun DistributionBar(
    label: String,
    value: Double,
    max: Double,
) {
    val ratio = (value / max).coerceIn(0.0, 1.0).toFloat()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label.padEnd(12), color = TermGreen, fontSize = 10.sp, modifier = Modifier.width(80.dp))
        Text("▕" + "█".repeat((ratio * 15).toInt()) + "░".repeat(15 - (ratio * 15).toInt()) + "▏", color = TermGreen)
    }
}

@Composable
fun Footer() {
    Text(
        "MATHEMATICAL MODELS ONLY. NOT FINANCIAL ADVICE.\nRUNNING 1000 MONTE CARLO SIMULATIONS... DONE.",
        color = TermGreen.copy(alpha = 0.5f),
        fontSize = 9.sp,
        modifier = Modifier.padding(top = 10.dp),
    )
}
