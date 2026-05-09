package com.cryptodept.domain.usecase

import javax.inject.Inject

/**
 * Generates prompts for image AI (Midjourney/DALL-E) to create YouTube thumbnails.
 */
class ThumbnailPromptBuilder
    @Inject
    constructor() {
        fun build(
            headline: String, // 3-word headline
            mainCoin: String,
            isBullish: Boolean,
        ): String {
            val neonColor = if (isBullish) "neon emerald green" else "vivid volcanic red"
            val palette = "Pure black background with $neonColor highlights and rim lighting"
            val style = "Visual style combining Mr Beast click-through optimization with Bloomberg's professional data aesthetic. Hyper-realistic 3D, Unreal Engine 5 render."

            return """
                {Subject: Massive hyper-detailed 3D $mainCoin coin as a central hero element}
                {Style: $style}
                {Palette: $palette}
                {Text Overlay: Large, chunky, glowing 3-word text in English: "$headline"}
                {Quality: 8k, photorealistic, ray-traced shadows, high contrast}
                """.trimIndent()
        }
    }
