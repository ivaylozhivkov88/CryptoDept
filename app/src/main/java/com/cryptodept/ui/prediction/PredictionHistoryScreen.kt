package com.cryptodept.ui.prediction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.cryptodept.data.db.PredictionAccuracyEntity
import com.cryptodept.ui.theme.LocalTerminalColors
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PredictionHistoryScreen(
    viewModel: PredictionViewModel,
    onBack: () -> Unit,
) {
    val history = viewModel.history.collectAsLazyPagingItems()
    val colors = LocalTerminalColors.current

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(16.dp),
    ) {
        Text(
            text = ">>> PREDICTION_VERIFICATION_LOG",
            color = colors.primary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                count = history.itemCount,
                key = history.itemKey { it.id },
            ) { index ->
                history[index]?.let { item ->
                    PredictionHistoryItem(item)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.background),
        ) {
            Text("< RETURN_TO_DASHBOARD")
        }
    }
}

@Composable
fun PredictionHistoryItem(item: PredictionAccuracyEntity) {
    val colors = LocalTerminalColors.current
    val dateStr = SimpleDateFormat("dd/MM HH:mm", Locale.US).format(Date(item.predictedAt))

    val statusColor =
        when {
            item.wasCorrect == true -> colors.primary
            item.wasCorrect == false -> colors.danger
            else -> colors.amber
        }

    val statusText =
        when {
            item.wasCorrect == true -> "VERIFIED_CORRECT"
            item.wasCorrect == false -> "VERIFIED_INCORRECT"
            else -> "PENDING_VERIFICATION"
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, colors.grid, RectangleShape)
                .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "[${item.coinId.uppercase()}]",
                color = colors.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                text = dateStr,
                color = colors.dimText,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "MODEL: ${item.model} | PREDICTED: ${item.predictedDirection}",
                color = colors.textPrimary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "STATUS: $statusText",
            color = statusColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
    }
}
