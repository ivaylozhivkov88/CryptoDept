package com.cryptodept.ui.tools

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.domain.model.CorrelationMatrix
import com.cryptodept.ui.components.TerminalLoadingSkeleton
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.viewmodel.CorrelationUiState
import com.cryptodept.viewmodel.CorrelationViewModel

@Composable
fun CorrelationScreen(
    viewModel: CorrelationViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val colors = LocalTerminalColors.current
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(16.dp),
    ) {
        Text(
            text = ">>> ASSET_CORRELATION_MATRIX_V4",
            color = colors.primary,
            fontFamily = FontFamily.Monospace,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "PEARSON COEFFICIENT (-1.0 TO +1.0) | 30D WINDOW",
            color = colors.dimText,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
        )

        HorizontalDivider(color = colors.grid, modifier = Modifier.padding(vertical = 12.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (val uiState = state) {
                is CorrelationUiState.Loading -> {
                    TerminalLoadingSkeleton()
                }
                is CorrelationUiState.Success -> {
                    CorrelationHeatMap(uiState.matrix)
                }
                is CorrelationUiState.Error -> {
                    Text("ERROR: ${uiState.message}", color = colors.danger, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, colors.primary),
            shape = RectangleShape,
        ) {
            Text("RETURN_TO_TERMINAL", color = colors.primary, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun CorrelationHeatMap(matrix: CorrelationMatrix) {
    val colors = LocalTerminalColors.current
    val symbols = matrix.symbols
    val n = symbols.size

    val cellSize = 50.dp
    val totalSize = cellSize * (n + 1)

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
                .verticalScroll(rememberScrollState()),
    ) {
        Canvas(modifier = Modifier.size(totalSize)) {
            val pxCellSize = cellSize.toPx()

            // Draw Labels
            symbols.forEachIndexed { i, symbol ->
                // Top labels (horizontal axis)
                drawIntoCanvas { canvas ->
                    val paint =
                        android.graphics.Paint().apply {
                            color = colors.primary.toArgb()
                            textSize = 24f
                            typeface = android.graphics.Typeface.MONOSPACE
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    canvas.nativeCanvas.drawText(
                        symbol,
                        (i + 1) * pxCellSize + (pxCellSize / 2),
                        pxCellSize * 0.7f,
                        paint,
                    )
                }

                // Left labels (vertical axis)
                drawIntoCanvas { canvas ->
                    val paint =
                        android.graphics.Paint().apply {
                            color = colors.primary.toArgb()
                            textSize = 24f
                            typeface = android.graphics.Typeface.MONOSPACE
                        }
                    canvas.nativeCanvas.drawText(
                        symbol,
                        pxCellSize * 0.1f,
                        (i + 1) * pxCellSize + (pxCellSize * 0.6f),
                        paint,
                    )
                }
            }

            // Draw Grid Cells
            for (i in 0 until n) {
                for (j in 0 until n) {
                    val correlation = matrix.matrix[i][j]

                    // Color Logic
                    val cellColor =
                        when {
                            correlation > 0.8 -> colors.primary // Strong positive
                            correlation > 0.5 -> colors.primary.copy(alpha = 0.6f) // Moderate positive
                            correlation < -0.8 -> colors.danger // Strong negative
                            correlation < -0.5 -> colors.danger.copy(alpha = 0.6f) // Moderate negative
                            else -> colors.grid.copy(alpha = 0.2f) // Weak/Neutral
                        }

                    drawRect(
                        color = cellColor,
                        topLeft = Offset((j + 1) * pxCellSize, (i + 1) * pxCellSize),
                        size = Size(pxCellSize - 4, pxCellSize - 4),
                    )

                    // Draw Value Text
                    drawIntoCanvas { canvas ->
                        val textPaint =
                            android.graphics.Paint().apply {
                                color = (if (kotlin.math.abs(correlation) > 0.6) Color.Black else colors.primary).toArgb()
                                textSize = 22f
                                textAlign = android.graphics.Paint.Align.CENTER
                                typeface =
                                    android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                            }
                        val displayVal = String.format(java.util.Locale.US, "%.2f", correlation)
                        canvas.nativeCanvas.drawText(
                            displayVal,
                            (j + 1) * pxCellSize + (pxCellSize / 2),
                            (i + 1) * pxCellSize + (pxCellSize / 1.6f),
                            textPaint,
                        )
                    }
                }
            }
        }
    }
}
