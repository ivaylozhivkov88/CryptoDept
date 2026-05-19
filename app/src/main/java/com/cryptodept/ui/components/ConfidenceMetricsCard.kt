package com.cryptodept.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.domain.prediction.ConfidenceMetrics
import com.cryptodept.domain.prediction.DataQuality
import com.cryptodept.domain.prediction.HistoricalAccuracy
import com.cryptodept.domain.prediction.ModelAgreement
import com.cryptodept.domain.prediction.VolatilityLevel
import com.cryptodept.ui.theme.LocalTerminalColors

@Composable
fun ConfidenceMetricsCard(
    metrics: ConfidenceMetrics,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTerminalColors.current
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, colors.grid, RectangleShape)
            .background(colors.background.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = ">>> CONFIDENCE_METRICS",
            color = colors.primary,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        
        HorizontalDivider(color = colors.grid, thickness = 1.dp)
        
        ConfidenceBar(value = metrics.overallConfidence)
        
        MetricRow(
            label = "MODEL_AGREEMENT",
            value = "${metrics.modelAgreement.emoji} ${metrics.modelAgreement.displayName}",
            valueColor = when (metrics.modelAgreement) {
                ModelAgreement.STRONG -> colors.primary
                ModelAgreement.MODERATE -> colors.amber
                ModelAgreement.WEAK -> colors.amber
                ModelAgreement.NONE -> colors.danger
            },
        )
        
        MetricRow(
            label = "DATA_QUALITY",
            value = metrics.dataQuality.displayName,
            valueColor = when (metrics.dataQuality) {
                DataQuality.HIGH -> colors.primary
                DataQuality.MEDIUM -> colors.amber
                DataQuality.LOW -> colors.danger
                DataQuality.INSUFFICIENT -> colors.dimText
            },
        )
        
        if (metrics.volatilityWarning == VolatilityLevel.HIGH ||
            metrics.volatilityWarning == VolatilityLevel.EXTREME) {
            MetricRow(
                label = "⚠️ VOLATILITY",
                value = metrics.volatilityWarning.warning ?: metrics.volatilityWarning.displayName,
                valueColor = colors.danger,
            )
        }
        
        metrics.invalidationLevel?.let { invalidPrice ->
            MetricRow(
                label = "INVALIDATION",
                value = "↓ $${"%,.2f".format(invalidPrice)}",
                valueColor = colors.amber,
            )
        }
        
        metrics.historicalAccuracy?.let { accuracy ->
            if (accuracy.isReliable) {
                Spacer(modifier = Modifier.height(4.dp))
                AccuracyBar(accuracy = accuracy)
            }
        }
        
        Text(
            text = "Statistical estimate. Not financial advice.",
            color = colors.dimText,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun ConfidenceBar(value: Float) {
    val colors = LocalTerminalColors.current
    val percentage = (value * 100).toInt()
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "CONFIDENCE",
                color = colors.textPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
            Text(
                text = "$percentage%",
                color = when {
                    percentage >= 70 -> colors.primary
                    percentage >= 50 -> colors.amber
                    else -> colors.danger
                },
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = generateAsciiBar(value, length = 20),
            color = colors.primary,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun AccuracyBar(accuracy: HistoricalAccuracy) {
    val colors = LocalTerminalColors.current
    val percent = accuracy.accuracyPercent.toInt()
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "HISTORICAL_ACCURACY",
                color = colors.textPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
            Text(
                text = "$percent% (${accuracy.timeframeText})",
                color = when {
                    percent >= 60 -> colors.primary
                    percent >= 45 -> colors.amber
                    else -> colors.danger
                },
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = generateAsciiBar(accuracy.accuracyPercent / 100f, length = 20),
            color = colors.amber,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun MetricRow(label: String, value: String, valueColor: Color) {
    val colors = LocalTerminalColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = colors.textPrimary,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
        )
        Text(
            text = value,
            color = valueColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun generateAsciiBar(value: Float, length: Int): String {
    val filled = (value.coerceIn(0f, 1f) * length).toInt()
    val empty = length - filled
    return "▰".repeat(filled) + "▱".repeat(empty)
}
