package com.cryptodept.ui.tutorial

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import com.cryptodept.domain.tutorial.TutorialTargetId
import kotlin.math.roundToInt

/**
 * Displays a demo screenshot image overlaid exactly on the spotlight target area.
 *
 * FIX EXPLANATION:
 * Previous implementation used AnimatedVisibility which creates its own layout
 * scope without fillMaxSize(). This caused offset() to be calculated relative
 * to the wrong parent, placing the image off-screen.
 *
 * Correct approach:
 * 1. Outer Box fills the entire screen (same as TutorialSpotlight Canvas)
 * 2. Image uses a custom layout modifier to place itself at exact pixel coordinates
 * 3. No AnimatedVisibility — avoids layout scope issues
 */
@Composable
fun TutorialDemoImage(
    targetId: TutorialTargetId,
    targetBounds: Rect?,
) {
    // Early exit if no bounds or no image defined for this step
    if (targetBounds == null || targetBounds == Rect.Zero) return
    val imageResId = TutorialTargetImages.imageForTarget(targetId) ?: return

    // CRITICAL: Box must be fillMaxSize so we have a full screen coordinate system.
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = imageResId),
            contentDescription = "Demo preview for ${targetId.name}",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                // Custom layout modifier places the image at exact screen coordinates (pixels),
                // ignoring any padding or offsets of parent.
                .layout { measurable, constraints ->
                    val widthPx = targetBounds.width.roundToInt()
                    val heightPx = targetBounds.height.roundToInt()
                    val placeable = measurable.measure(
                        constraints.copy(
                            minWidth = widthPx,
                            maxWidth = widthPx,
                            minHeight = heightPx,
                            maxHeight = heightPx,
                        )
                    )
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(
                            x = targetBounds.left.roundToInt(),
                            y = targetBounds.top.roundToInt(),
                        )
                    }
                }
        )
    }
}
