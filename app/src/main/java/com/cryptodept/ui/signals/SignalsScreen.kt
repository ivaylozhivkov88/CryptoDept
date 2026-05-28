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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.domain.model.Sentiment
import com.cryptodept.domain.model.SignalStrength
import com.cryptodept.domain.usecase.AlphaSignal
import com.cryptodept.domain.usecase.SignalType
import com.cryptodept.ui.components.TerminalLoadingSkeleton
import com.cryptodept.ui.tutorial.tutorialTarget
import com.cryptodept.domain.tutorial.TutorialTargetId
import com.cryptodept.ui.theme.GreenPrimary
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.ui.theme.TerminalRed
import com.cryptodept.viewmodel.CoinSignal
import com.cryptodept.viewmodel.SignalsViewModel
import java.util.Locale

@Composable
fun SignalsScreen(viewModel: SignalsViewModel = hiltViewModel()) {
    val signals by viewModel.signals.collectAsState()
    val alphaSignals by viewModel.alphaSignals.collectAsState()
    val isPro by viewModel.isPro.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val colors = LocalTerminalColors.current

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(8.dp)
                .tutorialTarget(TutorialTargetId.SIGNALS_LIST),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "--- ALPHA_INTELLIGENCE_FEED [PRO] ---",
                color = colors.amber,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .tutorialTarget(TutorialTargetId.SIGNALS_COMPOSER),
            )
        }

        if (alphaSignals.isNotEmpty()) {
            items(alphaSignals) { alpha ->
                AlphaSignalRow(alpha, isPro)
            }
        } else if (isLoading) {
            item { TerminalLoadingSkeleton(modifier = Modifier.height(80.dp)) }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "--- COMPOSITE_MARKET_SIGNALS ---",
                color = colors.primary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        if (isLoading && signals.isEmpty()) {
            items(3) {
                TerminalLoadingSkeleton(modifier = Modifier.height(100.dp).padding(vertical = 4.dp))
            }
        } else {
            items(signals) { signal ->
                SignalRow(signal)
            }
        }
    }
}

@Composable
fun AlphaSignalRow(signal: AlphaSignal, isPro: Boolean) {
    val colors = LocalTerminalColors.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.amber, RectangleShape)
            .background(if (isPro) colors.amber.copy(alpha = 0.05f) else Color.DarkGray.copy(alpha = 0.2f))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "★ ALPHA_SIGNAL",
                color = colors.amber,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "STRENGTH: ${signal.strength}%",
                color = colors.amber,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        
        Text(
            text = "TYPE: ${signal.type.name.replace("_", " ")}",
            color = colors.textPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = FontFamily.Monospace
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        if (isPro) {
            Text(
                text = signal.reason,
                color = colors.primary,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .background(colors.grid.copy(alpha = 0.5f))
            ) {
                Text(
                    text = "CONFIDENTIAL_DATA_OBFUSCATED_UPGRADE_TO_PRO",
                    color = Color.Black,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun SignalRow(coinSignal: CoinSignal) {
    val signal = coinSignal.signal
    val colors = LocalTerminalColors.current

    val strengthColor =
        when (signal.strength) {
            SignalStrength.STRONG_BUY, SignalStrength.BUY -> colors.primary
            SignalStrength.STRONG_SELL, SignalStrength.SELL -> colors.error
            else -> colors.dimText
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, strengthColor.copy(alpha = 0.5f))
                .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "⚡ NEW_SIGNAL: ${coinSignal.coinId.uppercase()}",
                color = colors.primary,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
            )
            Text(
                text = signal.strength.name.replace("_", " "),
                color = strengthColor,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            signal.indicators.forEach { ind ->
                val indColor =
                    when (ind.sentiment) {
                        Sentiment.BULLISH -> colors.primary
                        Sentiment.BEARISH -> colors.error
                        else -> colors.dimText
                    }
                Text(
                    text = "${ind.name}: ${ind.value}  ",
                    color = indColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Confidence Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.tutorialTarget(TutorialTargetId.SIGNALS_PERFORMANCE)
        ) {
            Text("CONFIDENCE: ", color = colors.dimText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            val dots = (signal.confidence * 10).toInt().coerceIn(0, 10)
            Text(
                text = "█".repeat(dots) + "░".repeat(10 - dots) + " ${(signal.confidence * 100).toInt()}%",
                color = colors.primary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
