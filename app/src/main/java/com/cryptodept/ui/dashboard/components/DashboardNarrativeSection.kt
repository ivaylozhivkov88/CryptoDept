package com.cryptodept.ui.dashboard.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.ui.theme.LocalTerminalColors

@Composable
fun DashboardNarrativeSection(
    aiSummary: String,
    dailyPickSymbol: String,
    dailyPickDirection: String,
    dailyPickConfidence: Int,
    onExpandNarrative: () -> Unit,
    onAiPickAccuracyClick: () -> Unit,
    onAiPickExpand: () -> Unit
) {
    Column {
        OracleNarrativeStrip(
            narrative = aiSummary,
            onExpand = onExpandNarrative
        )

        Spacer(Modifier.height(4.dp))

        AiPickStrip(
            symbol = dailyPickSymbol,
            direction = dailyPickDirection,
            confidence = dailyPickConfidence,
            onExpand = onAiPickExpand,
            onAccuracyClick = onAiPickAccuracyClick
        )
    }
}

@Composable
fun OracleNarrativeStrip(
    narrative: String,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalTerminalColors.current
    val isError = narrative.contains("SIGNAL_LOST")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .border(0.5.dp, if (isError) colors.danger else colors.primary, RectangleShape)
            .clickable { onExpand() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = ">>> SENTINEL",
            color = if (isError) colors.danger else colors.primary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )

        val statusText = when {
            isError -> "CRITICAL: SIGNAL LOST"
            narrative.contains("VERDICT:") -> narrative.substringAfter("VERDICT:").substringBefore("\n").trim()
            else -> narrative.take(30).plus("...")
        }

        Text(
            text = statusText.uppercase(),
            color = Color.White,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
        )

        Text(
            text = "[ EXPAND → ]",
            color = if (isError) colors.danger else colors.primary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun AiPickStrip(
    symbol: String,
    direction: String,
    confidence: Int,
    onExpand: () -> Unit,
    onAccuracyClick: () -> Unit
) {
    val colors = LocalTerminalColors.current
    val directionColor = when (direction.uppercase()) {
        "BULLISH" -> colors.primary
        "BEARISH" -> colors.danger
        else -> Color.White
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .border(0.5.dp, colors.amber, RectangleShape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = ">>> AI_PICK",
            color = colors.amber,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onAccuracyClick() }
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp).clickable { onAccuracyClick() }
        ) {
            Text(
                text = "$symbol ● ",
                color = Color.White,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = direction.uppercase(),
                color = directionColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "  $confidence%",
                color = Color.White,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Text(
            text = "[ TRACK_RECORD → ]",
            color = colors.amber,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.clickable { onExpand() }
        )
    }
}
