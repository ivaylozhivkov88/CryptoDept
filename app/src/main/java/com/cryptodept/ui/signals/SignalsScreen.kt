package com.cryptodept.ui.signals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.domain.model.Sentiment
import com.cryptodept.domain.model.SignalStrength
import com.cryptodept.ui.components.TerminalLoadingSkeleton
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.ui.theme.GreenPrimary
import com.cryptodept.ui.theme.TerminalRed
import com.cryptodept.viewmodel.SignalsViewModel
import com.cryptodept.viewmodel.CoinSignal

@Composable
fun SignalsScreen(
    viewModel: SignalsViewModel = hiltViewModel()
) {
    val signals by viewModel.signals.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val colors = LocalTerminalColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(8.dp)
    ) {
        Text(
            text = "--- COMPOSITE_SIGNAL_FEED [v2.0] ---",
            color = colors.primary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (isLoading) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(5) {
                    TerminalLoadingSkeleton(modifier = Modifier.height(100.dp).padding(vertical = 4.dp))
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(signals) { signal ->
                    SignalRow(signal)
                }
            }
        }
    }
}

@Composable
fun SignalRow(coinSignal: CoinSignal) {
    val signal = coinSignal.signal
    val colors = LocalTerminalColors.current

    val strengthColor = when (signal.strength) {
        SignalStrength.STRONG_BUY, SignalStrength.BUY -> GreenPrimary
        SignalStrength.STRONG_SELL, SignalStrength.SELL -> TerminalRed
        else -> colors.dimText
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, strengthColor.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚡ NEW_SIGNAL: ${coinSignal.coinId.uppercase()}",
                color = colors.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = signal.strength.name.replace("_", " "),
                color = strengthColor,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            signal.indicators.forEach { ind ->
                val indColor = when (ind.sentiment) {
                    Sentiment.BULLISH -> GreenPrimary
                    Sentiment.BEARISH -> TerminalRed
                    else -> colors.dimText
                }
                Text(
                    text = "${ind.name}: ${ind.value}  ",
                    color = indColor,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Confidence Bar
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("CONFIDENCE: ", color = colors.dimText, fontSize = 10.sp)
            val dots = (signal.confidence * 10).toInt().coerceIn(0, 10)
            Text(
                text = "█".repeat(dots) + "░".repeat(10 - dots) + " ${(signal.confidence * 100).toInt()}%",
                color = colors.primary,
                fontSize = 10.sp
            )
        }
    }
}