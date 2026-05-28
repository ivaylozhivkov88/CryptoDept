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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    coinId: String,
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
        Row(
            modifier = Modifier.fillMaxWidth(), 
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (expanded) "[-] $modelName" else "[+] $modelName",
                    color = if (expanded) colors.primary else colors.textPrimary.copy(alpha = 0.8f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                com.cryptodept.ui.components.ModelAccuracyBadge(
                    modelName = modelName,
                    coinId = coinId
                )
            }

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
