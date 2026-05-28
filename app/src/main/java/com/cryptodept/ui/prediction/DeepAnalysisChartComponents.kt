package com.cryptodept.ui.prediction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.domain.model.PricePrediction
import com.cryptodept.ui.theme.LocalTerminalColors
import java.util.Locale

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
