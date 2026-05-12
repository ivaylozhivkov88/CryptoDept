package com.cryptodept.ui.tutorial

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import com.cryptodept.domain.tutorial.TutorialTargetId

/**
 * In-memory registry mapping TutorialTargetId → bounding Rect on screen.
 * Populated by tutorialTarget() Modifier during composition.
 */
class TutorialTargetRegistry {
    private val targets = mutableStateMapOf<TutorialTargetId, Rect>()

    fun register(id: TutorialTargetId, bounds: Rect) {
        targets[id] = bounds
    }

    fun unregister(id: TutorialTargetId) {
        targets.remove(id)
    }

    fun getBounds(id: TutorialTargetId): Rect? = targets[id]

    fun clear() = targets.clear()
}

val LocalTutorialTargetRegistry = compositionLocalOf<TutorialTargetRegistry?> { null }

/**
 * Marks this Composable as a target for the tutorial overlay.
 * The overlay will spotlight this area and anchor the tooltip near it.
 *
 * Usage:
 *   Card(modifier = Modifier.tutorialTarget(TutorialTargetId.DASH_AI_NARRATIVE)) { ... }
 */
fun Modifier.tutorialTarget(id: TutorialTargetId): Modifier = composed {
    val registry = LocalTutorialTargetRegistry.current
    this.onGloballyPositioned { coords ->
        registry?.register(id, coords.boundsInRoot())
    }
}
