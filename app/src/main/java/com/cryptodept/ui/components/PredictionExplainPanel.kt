package com.cryptodept.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.domain.model.Direction
import com.cryptodept.domain.model.PredictionFactor
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.util.TerminalConfig

@Composable
fun PredictionExplainPanel(
    factors: List<PredictionFactor>,
    modifier: Modifier = Modifier,
) {
    val colors = LocalTerminalColors.current
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Expand/collapse button
        Row(
            modifier = Modifier
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (isExpanded) "[ EXPLAIN ▲ ]" else "[ EXPLAIN › ]",
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.grid, RectangleShape)
                    .background(colors.surface)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = ">>> SIGNAL_BREAKDOWN",
                    color = colors.dimText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                )

                factors.forEach { factor ->
                    val directionColor = when (factor.direction) {
                        Direction.UP, Direction.STRONG_UP -> colors.primary
                        Direction.DOWN, Direction.STRONG_DOWN -> colors.error
                        else -> colors.dimText
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "• ${factor.label}",
                                color = colors.textPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                            )
                            Text(
                                text = "  ${factor.detail}",
                                color = colors.dimText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${factor.weightPercent}%",
                                color = colors.dimText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                            )
                            Text(
                                text = factor.direction.name,
                                color = directionColor,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                // Weight bar
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "WEIGHT DISTRIBUTION",
                    color = colors.dimText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                )
                Row(modifier = Modifier.fillMaxWidth().height(6.dp)) {
                    factors.forEach { factor ->
                        val barColor = when (factor.direction) {
                            Direction.UP, Direction.STRONG_UP -> colors.primary
                            Direction.DOWN, Direction.STRONG_DOWN -> colors.error
                            else -> colors.dimText
                        }
                        Box(
                            modifier = Modifier
                                .weight(factor.weightPercent.toFloat())
                                .fillMaxHeight()
                                .background(barColor.copy(alpha = 0.7f))
                        )
                    }
                }
            }
        }
    }
}
