package com.cryptodept.ui.tutorial

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Layout
import com.cryptodept.domain.tutorial.TourStep
import com.cryptodept.domain.tutorial.TutorialController

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

    Box(modifier = Modifier.fillMaxSize()) {

        // Layer 1: Dimmed spotlight
        TutorialSpotlight(targetBounds = targetBounds)

        // Layer 2: Tooltip positioned smartly
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

            // If no target or center-positioned, center vertically
            if (targetBounds == null) {
                placeable.place(
                    x = centerX,
                    y = (constraints.maxHeight - placeable.height) / 2
                )
                return@layout
            }

            // Decide: above or below the target?
            val screenHeight = constraints.maxHeight
            val spaceBelow = screenHeight - targetBounds.bottom
            val spaceAbove = targetBounds.top
            val gap = 24

            val y = if (spaceBelow >= placeable.height + gap) {
                // Place below
                (targetBounds.bottom + gap).toInt()
            } else if (spaceAbove >= placeable.height + gap) {
                // Place above
                (targetBounds.top - placeable.height - gap).toInt()
            } else {
                // Not enough space either way — center
                (screenHeight - placeable.height) / 2
            }

            placeable.place(x = centerX, y = y.coerceIn(20, screenHeight - placeable.height - 20))
        }
    }
}
