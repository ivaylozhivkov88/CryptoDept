package com.cryptodept.ui.tutorial

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.cryptodept.R
import com.cryptodept.domain.tutorial.TourStep
import com.cryptodept.ui.theme.LocalTerminalColors
import kotlinx.coroutines.delay

@Composable
fun TutorialTooltip(
    step: TourStep,
    stepNumber: Int,
    totalSteps: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSkip: () -> Unit,
    isFirstStep: Boolean,
    isLastStep: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalTerminalColors.current
    val haptic = LocalHapticFeedback.current
    val title = stringResource(step.titleKey)
    val message = stringResource(step.messageKey)

    // Typewriter effect for message
    var typedMessage by remember(step.id) { mutableStateOf("") }

    LaunchedEffect(step.id) {
        typedMessage = ""
        message.forEachIndexed { idx, _ ->
            typedMessage = message.substring(0, idx + 1)
            delay(15L)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth(0.92f)
            .border(
                width = 1.dp,
                color = colors.primary,
                shape = RectangleShape
            ),
        shape = RectangleShape,
        colors = CardDefaults.cardColors(
            containerColor = colors.background.copy(alpha = 0.98f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = colors.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "[${stepNumber}/${totalSteps}]",
                    color = colors.dimText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = colors.primary.copy(alpha = 0.3f), thickness = 1.dp)
            Spacer(Modifier.height(12.dp))

            // Message with typewriter effect
            Text(
                text = typedMessage,
                color = colors.textPrimary,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = colors.primary.copy(alpha = 0.3f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onSkip) {
                    Text(
                        text = stringResource(R.string.tutorial_skip),
                        color = colors.dimText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row {
                    if (!isFirstStep) {
                        TextButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onPrevious()
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.tutorial_previous),
                                color = colors.textPrimary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNext()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.background
                        ),
                        shape = RectangleShape,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            text = stringResource(
                                if (isLastStep) R.string.tutorial_finish
                                else R.string.tutorial_next
                            ),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}
