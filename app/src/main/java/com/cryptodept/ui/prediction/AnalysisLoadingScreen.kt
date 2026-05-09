package com.cryptodept.ui.prediction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun AnalysisLoadingScreen(state: AnalysisUiState.Loading) {
    val listState = rememberLazyListState()

    // Automatic scroll for each new log
    LaunchedEffect(state.logs.size) {
        if (state.logs.isNotEmpty()) {
            listState.animateScrollToItem(state.logs.size - 1)
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp),
    ) {
        Text(
            text = ">>> QUANTUM_CORE_SCANNER_V3",
            color = Color(0xFF00FF41),
            fontFamily = FontFamily.Monospace,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            itemsIndexed(state.logs) { index, log ->
                val isLast = index == state.logs.size - 1
                TerminalLogLine(
                    text = log,
                    isCurrent = isLast,
                )
            }
        }

        // Прогрес панел (от image_316f5a.png)
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
            Text(
                text = "ANALYSIS_PROGRESS: ${(state.progress * 100).toInt()}%",
                color = Color(0xFF00FF41),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = Color(0xFF00FF41),
                trackColor = Color(0xFF00FF41).copy(alpha = 0.1f),
            )
        }
    }
}

@Composable
fun TerminalLogLine(
    text: String,
    isCurrent: Boolean,
) {
    var displayedText by remember { mutableStateOf("") }

    // Ефект "Пишеща машина"
    LaunchedEffect(text) {
        displayedText = ""
        text.forEach { char ->
            displayedText += char
            delay(10) // По-бързо
        }
    }

    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = if (isCurrent) ">> " else "[ OK ] ",
            color = if (isCurrent) Color.White else Color(0xFF00FF41),
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
        )
        Text(
            text = displayedText,
            color = if (isCurrent) Color.White else Color(0xFF00FF41).copy(alpha = 0.7f),
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
        )
    }
}
