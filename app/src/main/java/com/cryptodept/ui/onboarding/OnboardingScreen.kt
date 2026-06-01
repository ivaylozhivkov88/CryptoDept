package com.cryptodept.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.util.TerminalConfig
import kotlinx.coroutines.delay

@Composable
fun OnboardingScreen(onOnboardingComplete: () -> Unit) {
    val colors = LocalTerminalColors.current
    var currentSlide by remember { mutableIntStateOf(1) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
    ) {
        Crossfade(targetState = currentSlide, label = "onboarding_fade") { slide ->
            when (slide) {
                1 -> SystemBootSlide { currentSlide = 2 }
                2 -> AgentDeploymentSlide { currentSlide = 3 }
                3 -> OperativeAgreementSlide(onOnboardingComplete)
            }
        }
    }
}

@Composable
private fun SystemBootSlide(onComplete: () -> Unit) {
    val colors = LocalTerminalColors.current
    val bootLogs = listOf(
        ">>> INITIALIZING_KERNEL_V1.5.0",
        ">>> MOUNTING_FIREBASE_CLOUDS...",
        ">>> DECRYPTING_AGENT_LOGIC_GATE...",
        ">>> SYNCING_GLOBAL_TICKER_NODES...",
        ">>> HANDSHAKE_ESTABLISHED_WITH_ORACLE",
        ">>> ENCRYPTING_USER_SESSION...",
        ">>> TERMINAL_READY_FOR_OPERATOR"
    )
    var visibleLines by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        bootLogs.forEach { _ ->
            visibleLines++
            delay(300)
        }
        delay(1000)
        onComplete()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        bootLogs.take(visibleLines).forEach { line ->
            Text(
                text = line,
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        BlinkingCursor()
    }
}

@Composable
private fun AgentDeploymentSlide(onComplete: () -> Unit) {
    val colors = LocalTerminalColors.current
    val agents = listOf(
        "AGENT-SENTINEL" to "Technical Analysis & Chart Confluences",
        "AGENT-SCOUT" to "On-Chain Whale Movement Tracking",
        "AGENT-QUANT" to "Multi-Model Price Forecasting",
        "AGENT-PULSE" to "Social Sentiment & Market Hysteria"
    )
    var deployedCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        delay(500)
        while (deployedCount < agents.size) {
            deployedCount++
            delay(800)
        }
        delay(1500)
        onComplete()
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Text(
            text = ">>> DEPLOYING_INTELLIGENCE_UNITS",
            color = colors.amber,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Spacer(Modifier.height(32.dp))
        
        agents.take(deployedCount).forEach { (name, desc) ->
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "[ ACTIVE ]", color = colors.primary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.width(8.dp))
                    Text(text = name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
                }
                Text(text = desc, color = colors.dimText, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun OperativeAgreementSlide(onFinish: () -> Unit) {
    val colors = LocalTerminalColors.current
    val uriHandler = LocalUriHandler.current
    var accepted by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = ">>> OPERATIVE_DISCLOSURE",
            color = colors.danger,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            fontFamily = FontFamily.Monospace
        )
        
        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .border(1.dp, colors.grid, RectangleShape)
                .background(colors.grid.copy(alpha = 0.05f))
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "1. This terminal provides QUANTITATIVE DATA only.\n\n" +
                       "2. All AI reports (Sentinel, Oracle, Pulse) are statistical models, NOT financial advice.\n\n" +
                       "3. High-volatility trading involves significant risk of capital loss.\n\n" +
                       "4. You acknowledge that past accuracy does not guarantee future results.\n\n" +
                       "5. Access to institutional-grade data (Whale Tracker) requires a valid Intelligence Pass.",
                color = colors.textPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth().clickable { accepted = !accepted },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = accepted,
                onCheckedChange = { accepted = it },
                colors = CheckboxDefaults.colors(checkedColor = colors.primary, uncheckedColor = colors.grid)
            )
            Text(
                text = "I CONFIRM MY STRATEGIC UNDERSTANDING",
                color = if (accepted) colors.primary else colors.dimText,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onFinish,
            enabled = accepted,
            shape = RectangleShape,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = Color.Black,
                disabledContainerColor = colors.grid
            )
        ) {
            Text(">>> ACCESS_TERMINAL_CORE", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        
        TextButton(
            onClick = { uriHandler.openUri("https://gist.githubusercontent.com/ivaylozhivkov88/147ca22ec93a2af3dd9224c69466af82/raw/") },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Review Privacy Protocol", color = colors.dimText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun BlinkingCursor() {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(500), repeatMode = RepeatMode.Reverse),
        label = "cursor_alpha"
    )
    Box(modifier = Modifier.size(10.dp, 16.dp).background(LocalTerminalColors.current.primary.copy(alpha = alpha)))
}
