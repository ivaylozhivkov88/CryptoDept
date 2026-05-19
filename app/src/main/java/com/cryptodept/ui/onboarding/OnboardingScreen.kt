package com.cryptodept.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cryptodept.R
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.util.TerminalConfig
import kotlinx.coroutines.delay

@Composable
fun OnboardingScreen(onOnboardingComplete: () -> Unit) {
    val colors = LocalTerminalColors.current
    var currentSlide by remember { mutableIntStateOf(1) }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(TerminalConfig.UI.DEFAULT_PADDING),
    ) {
        Crossfade(targetState = currentSlide, label = "slide_transition") { slide ->
            when (slide) {
                1 -> BootSequenceSlide { currentSlide = 2 }
                2 -> RiskDisclaimerSlide(onOnboardingComplete)
            }
        }
    }
}

@Composable
fun BootSequenceSlide(onComplete: () -> Unit) {
    val colors = LocalTerminalColors.current
    val lines =
        listOf(
            stringResource(R.string.onboarding_init),
            stringResource(R.string.onboarding_feed),
            stringResource(R.string.onboarding_decrypt),
            stringResource(R.string.onboarding_loading),
        )
    var displayedLines by remember { mutableStateOf(emptyList<String>()) }

    LaunchedEffect(Unit) {
        lines.forEach { line ->
            var currentText = ""
            line.forEach { char ->
                currentText += char
                if (displayedLines.isEmpty()) {
                    displayedLines = listOf(currentText)
                } else {
                    displayedLines = displayedLines.dropLast(1) + currentText
                }
                delay(TerminalConfig.Animation.TYPEWRITER_SPEED_MS)
            }
            displayedLines = displayedLines + ""
            delay(200)
        }
        delay(800)
        onComplete()
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .clickable { onComplete() },
        verticalArrangement = Arrangement.Center,
    ) {
        displayedLines.forEach { line ->
            Text(
                text = line,
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = TerminalConfig.UI.FONT_SIZE_MEDIUM,
                modifier = Modifier.padding(vertical = TerminalConfig.UI.SPACER_SMALL / 2),
            )
        }

        BlinkingCursor()
    }
}

@Composable
fun RiskDisclaimerSlide(onFinish: () -> Unit) {
    val colors = LocalTerminalColors.current
    val uriHandler = LocalUriHandler.current
    var accepted by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.onboarding_notice), 
            color = colors.amber, 
            fontWeight = FontWeight.Bold, 
            fontSize = TerminalConfig.UI.FONT_SIZE_HEADER
        )
        Spacer(modifier = Modifier.height(TerminalConfig.UI.SPACER_LARGE))

        val disclaimer =
            listOf(
                stringResource(R.string.onboarding_disclaimer_1),
                stringResource(R.string.onboarding_disclaimer_2),
                stringResource(R.string.onboarding_disclaimer_3),
                stringResource(R.string.onboarding_disclaimer_4),
            )

        disclaimer.forEach { line ->
            Text(
                text = line, 
                color = colors.amber, 
                fontSize = TerminalConfig.UI.FONT_SIZE_MEDIUM, 
                modifier = Modifier.padding(vertical = TerminalConfig.UI.SPACER_SMALL)
            )
        }

        Spacer(modifier = Modifier.height(TerminalConfig.UI.SPACER_LARGE * 2))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = accepted,
                onCheckedChange = { accepted = it },
                colors =
                    CheckboxDefaults.colors(
                        checkedColor = colors.primary,
                        uncheckedColor = colors.grid,
                        checkmarkColor = colors.background,
                    ),
            )
            Text(
                text = stringResource(R.string.onboarding_understand),
                color = colors.textPrimary,
                modifier = Modifier.clickable { accepted = !accepted },
            )
        }

        Spacer(modifier = Modifier.height(TerminalConfig.UI.SPACER_LARGE))

        // PRIVACY POLICY LINK ON ONBOARDING
        Text(
            text = stringResource(R.string.onboarding_privacy),
            color = colors.dimText,
            fontSize = TerminalConfig.UI.FONT_SIZE_SMALL,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .clickable { uriHandler.openUri("https://gist.githubusercontent.com/ivaylozhivkov88/147ca22ec93a2af3dd9224c69466af82/raw/") }
                .padding(vertical = TerminalConfig.UI.SPACER_MEDIUM)
        )

        Spacer(modifier = Modifier.height(TerminalConfig.UI.SPACER_LARGE))

        Button(
            onClick = {
                onFinish()
            },
            enabled = accepted,
            shape = RectangleShape,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.background,
                    disabledContainerColor = colors.grid,
                    disabledContentColor = colors.dimText,
                ),
            modifier = Modifier.fillMaxWidth().height(TerminalConfig.Interaction.TOUCH_TARGET_SIZE.dp),
        ) {
            Text(stringResource(R.string.onboarding_enter), fontFamily = FontFamily.Monospace)
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
        animationSpec =
            infiniteRepeatable(
                animation = tween(500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "cursor_alpha",
    )

    Box(
        modifier =
            Modifier
                .size(10.dp, 16.dp)
                .background(colors.primary.copy(alpha = alpha)),
    )
}
