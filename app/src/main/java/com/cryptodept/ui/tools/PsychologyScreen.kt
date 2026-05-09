package com.cryptodept.ui.tools

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.domain.model.AlertSeverity
import com.cryptodept.domain.model.PsychologyAlert
import com.cryptodept.domain.model.SessionStats
import com.cryptodept.ui.components.PsychologyLockOverlay
import com.cryptodept.ui.components.TerminalCard
import com.cryptodept.ui.theme.*
import com.cryptodept.viewmodel.PsychologyUiState
import com.cryptodept.viewmodel.PsychologyViewModel
import java.util.Locale

@Composable
fun PsychologyScreen(
    viewModel: PsychologyViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    var showLock by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = ">>> TRADER PSYCHOLOGY MONITOR",
                color = WallStreetGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(24.dp))

            when (val uiState = state) {
                is PsychologyUiState.Loading -> {
                    CircularProgressIndicator(color = WallStreetGreen, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                is PsychologyUiState.Success -> {
                    PsychologyContent(uiState.stats) { showLock = true }
                }
                is PsychologyUiState.Error -> {
                    Text(uiState.message, color = WallStreetRed, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("< BACK_TO_TOOLS", color = WallStreetGreen, fontFamily = FontFamily.Monospace)
            }
        }

        PsychologyLockOverlay(
            isVisible = showLock,
            onDismiss = { showLock = false },
        )
    }
}

@Composable
fun PsychologyContent(
    stats: SessionStats,
    onTakeBreak: () -> Unit,
) {
    // TODAY'S SESSION
    TerminalCard(title = "TODAY'S SESSION") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Trades: ${stats.tradesToday}", color = WallStreetWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Text(
                    "Win Rate: ${if (stats.tradesToday > 0) (stats.winsToday * 100 / stats.tradesToday) else 0}%",
                    color = TextGray,
                    fontSize = 11.sp,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "P&L: ${String.format(Locale.US, "$%.2f", stats.dayPnL)}",
                    color = if (stats.dayPnL >= 0) WallStreetGreen else WallStreetRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text("Avg time: ${stats.avgTimeBetweenTrades} min", color = TextGray, fontSize = 11.sp)
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // TILT SCORE
    val tiltColor =
        when {
            stats.tiltScore >= 70 -> WallStreetRed
            stats.tiltScore >= 40 -> WallStreetAmber
            else -> WallStreetGreen
        }

    TerminalCard(title = "TILT SCORE") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stats.tiltScore.toString() + "/100",
                color = tiltColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(end = 12.dp),
            )
            LinearProgressIndicator(
                progress = { stats.tiltScore / 100f },
                modifier = Modifier.weight(1f).height(8.dp),
                color = tiltColor,
                trackColor = GridGray,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Butt,
            )
        }
        if (stats.isTiltDetected) {
            Text(
                "⚠ TILT DETECTED — High emotional risk",
                color = WallStreetRed,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // ALERTS
    stats.alerts.forEach { alert ->
        PsychologyAlertView(alert)
        Spacer(modifier = Modifier.height(12.dp))
    }

    if (stats.alerts.isEmpty()) {
        Text("No emotional triggers detected. Maintain discipline.", color = TextGray, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
    }

    Spacer(modifier = Modifier.height(24.dp))

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onTakeBreak,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = WallStreetAmber, contentColor = Color.Black),
            shape = RectangleShape,
        ) {
            Text("TAKE A BREAK", fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = { /* Continue */ },
            modifier = Modifier.weight(1f),
            border = BorderStroke(1.dp, TextGray),
            shape = RectangleShape,
        ) {
            Text("IGNORE", color = TextGray)
        }
    }
}

@Composable
fun PsychologyAlertView(alert: PsychologyAlert) {
    val color =
        when (alert.severity) {
            AlertSeverity.CRITICAL -> WallStreetRed
            AlertSeverity.WARNING -> WallStreetAmber
            AlertSeverity.INFO -> WallStreetGreen
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, color)
                .background(color.copy(alpha = 0.05f))
                .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (alert.severity == AlertSeverity.CRITICAL) "🔴 CRITICAL: " else "⚠ WARNING: ",
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
            )
            Text(text = alert.title, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = alert.detail, color = WallStreetWhite, fontSize = 11.sp, lineHeight = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "→ ${alert.recommendation}", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
