package com.cryptodept.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import com.cryptodept.ui.theme.LocalTerminalColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit
) {
    val colors = LocalTerminalColors.current
    var currentSlide by remember { mutableIntStateOf(1) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(24.dp)
    ) {
        Crossfade(targetState = currentSlide, label = "slide_transition") { slide ->
            when (slide) {
                1 -> BootSequenceSlide { currentSlide = 2 }
                2 -> NavigationSlide { currentSlide = 3 }
                3 -> RiskDisclaimerSlide(onOnboardingComplete)
            }
        }
    }
}

@Composable
fun BootSequenceSlide(onComplete: () -> Unit) {
    val colors = LocalTerminalColors.current
    val lines = listOf(
        "INITIALIZING CRYPTODEPT v3.0...",
        "CONNECTING TO MARKET FEED...",
        "LOADING TERMINAL..."
    )
    var displayedLines by remember { mutableStateOf(emptyList<String>()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        lines.forEach { line ->
            var currentText = ""
            line.forEach { char ->
                currentText += char
                if (displayedLines.isEmpty()) displayedLines = listOf(currentText)
                else displayedLines = displayedLines.dropLast(1) + currentText
                delay(30)
            }
            displayedLines = displayedLines + ""
            delay(300)
        }
        delay(1000)
        onComplete()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onComplete() },
        verticalArrangement = Arrangement.Center
    ) {
        displayedLines.forEach { line ->
            Text(
                text = line,
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
        
        BlinkingCursor()
    }
}

@Composable
fun NavigationSlide(onNext: () -> Unit) {
    val colors = LocalTerminalColors.current
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        Text(">>> HOW TO NAVIGATE", color = colors.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("TYPE COMMANDS OR USE QUICK ACCESS BUTTONS", color = colors.textPrimary, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("CHART BTC", "SIZER", "RISK", "JOURNAL").forEach { cmd ->
                Box(
                    modifier = Modifier
                        .border(1.dp, colors.grid, RectangleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = "[$cmd]", color = colors.primary, fontSize = 12.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onNext,
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.background),
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("[CONTINUE →]")
        }
    }
}

@Composable
fun RiskDisclaimerSlide(onFinish: () -> Unit) {
    val colors = LocalTerminalColors.current
    var accepted by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        Text(">>> IMPORTANT NOTICE", color = colors.amber, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(24.dp))
        
        val disclaimer = listOf(
            "THIS APP PROVIDES MARKET DATA AND ANALYSIS TOOLS ONLY.",
            "IT IS NOT FINANCIAL ADVICE.",
            "TRADING INVOLVES SIGNIFICANT RISK OF LOSS.",
            "NEVER RISK MORE THAN YOU CAN AFFORD TO LOSE."
        )
        
        disclaimer.forEach { line ->
            Text(line, color = colors.amber, fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp))
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = accepted,
                onCheckedChange = { accepted = it },
                colors = CheckboxDefaults.colors(
                    checkedColor = colors.primary,
                    uncheckedColor = colors.grid,
                    checkmarkColor = colors.background
                )
            )
            Text(
                "I UNDERSTAND THE RISKS", 
                color = colors.textPrimary, 
                modifier = Modifier.clickable { accepted = !accepted }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onFinish,
            enabled = accepted,
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary, 
                contentColor = colors.background,
                disabledContainerColor = colors.grid,
                disabledContentColor = colors.dimText
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("[ENTER THE TERMINAL →]")
        }
    }
}

@Composable
fun BlinkingCursor() {
    val colors = LocalTerminalColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )
    
    Box(
        modifier = Modifier
            .size(10.dp, 16.dp)
            .background(colors.primary.copy(alpha = alpha))
    )
}
