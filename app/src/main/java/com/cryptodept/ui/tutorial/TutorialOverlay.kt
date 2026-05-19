package com.cryptodept.ui.tutorial

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Layout
import com.cryptodept.domain.tutorial.TourStep
import com.cryptodept.domain.tutorial.TutorialController
import com.cryptodept.domain.tutorial.TutorialTargetId

/**
 * Renders the spotlight + tooltip overlay when tutorial is active.
 * Should be placed at the ROOT of the navigation graph,
 * ABOVE all screens (last in Box stack).
 */
@Composable
fun TutorialOverlay(
    controller: TutorialController,
    registry: TutorialTargetRegistry
) {
    val state by controller.state.collectAsState()

    if (!state.isActive || state.currentStep == null) return

    val step = state.currentStep!!
    val targetBounds: Rect? = registry.getBounds(step.target)
    val isActuallyDemo = state.isActive

    Box(modifier = Modifier.fillMaxSize()) {

        // Layer 1: Dimmed spotlight
        TutorialSpotlight(targetBounds = targetBounds)

        // Layer 2: Demo image (Overlays the hole in the spotlight)
        TutorialDemoImage(
            targetId = step.target,
            targetBounds = targetBounds
        )

        // Layer 3: Tooltip positioned smartly
        TooltipPositioner(
            targetBounds = targetBounds,
            modifier = Modifier.fillMaxSize()
        ) {
            TutorialTooltip(
                step = step,
                stepNumber = state.currentStepIndex + 1,
                totalSteps = state.totalSteps,
                onNext = { controller.nextStep() },
                onPrevious = { controller.previousStep() },
                onSkip = { controller.requestSkip() },
                isFirstStep = state.currentStepIndex == 0,
                isLastStep = state.currentStepIndex == state.totalSteps - 1
            )
        }
    }
}

/**
 * Places the tooltip either above or below the target,
 * depending on screen position.
 */
@Composable
private fun TooltipPositioner(
    targetBounds: Rect?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeable = measurables.firstOrNull()?.measure(
            constraints.copy(minWidth = 0, minHeight = 0)
        )

        layout(constraints.maxWidth, constraints.maxHeight) {
            if (placeable == null) return@layout

            val centerX = (constraints.maxWidth - placeable.width) / 2
            val screenHeight = constraints.maxHeight
            val gap = 32 // По-голямо разстояние за по-добра видимост

            // Ако няма цел, центрираме вертикално
            if (targetBounds == null) {
                placeable.place(x = centerX, y = (screenHeight - placeable.height) / 2)
                return@layout
            }

            val targetHeight = targetBounds.height
            val isLargeTarget = targetHeight > screenHeight * 0.6f

            val y = if (isLargeTarget) {
                // ЗА ГОЛЕМИ ПОЛЕТА: Поставяме обяснението ВЪРХУ тях (центрирано)
                (screenHeight - placeable.height) / 2
            } else {
                // ЗА МАЛКИ ПОЛЕТА: Местим обяснението ОТВЪН
                val spaceBelow = screenHeight - targetBounds.bottom
                val spaceAbove = targetBounds.top

                if (spaceBelow >= placeable.height + gap) {
                    // Има място отдолу -> Слагаме го там
                    (targetBounds.bottom + gap).toInt()
                } else if (spaceAbove >= placeable.height + gap) {
                    // Има място отгоре -> Слагаме го там
                    (targetBounds.top - placeable.height - gap).toInt()
                } else {
                    // Ако по някаква причина няма място и от двете страни (рядко) -> центрираме
                    (screenHeight - placeable.height) / 2
                }
            }

            // Ограничаваме 'y' в рамките на екрана с лек padding
            val safeY = y.coerceIn(24, screenHeight - placeable.height - 24)
            placeable.place(x = centerX, y = safeY)
        }
    }
}

/**
 * Provides static sample text for key UI elements during the tour.
 */
private fun getSampleTextForTarget(targetId: TutorialTargetId): String? = when (targetId) {
    TutorialTargetId.DASH_PRICE_TICKER -> "BTC ${'$'}103,245.50 ▲2.34%   ETH ${'$'}3,425.80 ▲1.85%"
    TutorialTargetId.DASH_AI_NARRATIVE -> "AI_NARRATIVE: BTC defends critical support. Ensemble confidence: 73%."
    TutorialTargetId.DASH_SENTIMENT_GAUGE -> "FEAR/GREED: 62 (GREED) | REDDIT: 68% BULLISH"
    TutorialTargetId.DASH_NETWORK_HEALTH -> "BTC HASH: 12 sat | ETH GAS: 24 gwei | MEMPOOL: 8420 txs"
    else -> null
}
