package com.cryptodept.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.domain.model.AnalysisTrace
import com.cryptodept.domain.model.TraceIntensity
import com.cryptodept.ui.theme.LocalTerminalColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechnicalBreakdownSheet(
    traces: List<AnalysisTrace>,
    onDismiss: () -> Unit,
) {
    val colors = LocalTerminalColors.current
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.background,
        shape = RectangleShape,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.grid) },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 32.dp),
        ) {
            Text(
                text = ">>> SIGNAL_REASONING_TRACE",
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )

            HorizontalDivider(color = colors.grid, modifier = Modifier.padding(vertical = 12.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(traces) { trace ->
                    TraceItem(trace)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary.copy(alpha = 0.1f)),
            ) {
                Text("DISMISS", color = colors.primary, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun TraceItem(trace: AnalysisTrace) {
    val colors = LocalTerminalColors.current
    val intensityColor =
        when (trace.intensity) {
            TraceIntensity.EXTREME -> colors.danger
            TraceIntensity.HIGH -> colors.primary
            TraceIntensity.MEDIUM -> colors.amber
            TraceIntensity.LOW -> colors.dimText
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, colors.grid)
                .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = trace.factor,
                color = intensityColor,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
            )
            Text(
                text = "[${trace.label}]",
                color = intensityColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = trace.reasoning,
            color = colors.textPrimary,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Small bar showing impact
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(colors.grid)) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(trace.score / 100f)
                        .fillMaxHeight()
                        .background(intensityColor),
            )
        }
    }
}
