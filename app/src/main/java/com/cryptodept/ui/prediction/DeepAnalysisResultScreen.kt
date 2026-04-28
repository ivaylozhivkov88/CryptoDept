package com.cryptodept.ui.prediction

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.domain.model.*
import java.util.Locale

@Composable
fun DeepAnalysisResultScreen(prediction: PricePrediction) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        item {
            ConsensusHeader(prediction)
            Spacer(modifier = Modifier.height(24.dp))
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
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "AGGREGATED_CONFIDENCE: ${(prediction.ensembleConsensus.overallConfidence * 100).toInt()}%", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
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