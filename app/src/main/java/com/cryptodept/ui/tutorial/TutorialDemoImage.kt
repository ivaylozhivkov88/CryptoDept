package com.cryptodept.ui.tutorial

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.layout
import com.cryptodept.domain.tutorial.TutorialTargetId
import kotlin.math.roundToInt

/**
 * Вместо да показва статичен PNG файл, този компонент рендира
 * "Жив Макет" (Mock View) директно в светещите очертания.
 */
@Composable
fun TutorialDemoImage(
    targetId: TutorialTargetId,
    targetBounds: Rect?,
) {
    if (targetBounds == null || targetBounds == Rect.Zero) return

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val widthPx = targetBounds.width.roundToInt()
                    val heightPx = targetBounds.height.roundToInt()
                    val placeable = measurable.measure(
                        constraints.copy(
                            minWidth = widthPx, maxWidth = widthPx,
                            minHeight = heightPx, maxHeight = heightPx
                        )
                    )
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(
                            x = targetBounds.left.roundToInt(),
                            y = targetBounds.top.roundToInt()
                        )
                    }
                }
        ) {
            // Тук избираме кой макет да "нарисуваме" в дупката
            when (targetId) {
                TutorialTargetId.DASH_PRICE_TICKER -> MockStep1_Ticker()
                TutorialTargetId.DASH_SENTIMENT_GAUGE -> MockStep2_Gauges()
                TutorialTargetId.DASH_AI_NARRATIVE -> MockStep3_Sentinel()
                TutorialTargetId.DASH_NETWORK_HEALTH -> MockStep4_Status()
                else -> { /* No mock for this target */ }
            }
        }
    }
}
