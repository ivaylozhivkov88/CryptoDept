package com.cryptodept.domain.usecase

import javax.inject.Inject

/**
 * Generates visual prompts for AI video generators (Sora, Runway, Veo)
 * based on current market sentiment.
 */
class VideoPromptBuilder
    @Inject
    constructor() {
        fun build(
            sentimentScore: Int, // 0-100
            mainAsset: String,
            isAggressive: Boolean = false,
        ): String {
            val isBullish = sentimentScore >= 55

            val subject = "A stylized $mainAsset holographic emblem"

            val action =
                when {
                    sentimentScore > 75 -> "ascending rapidly through a vortex of golden energy"
                    sentimentScore in 55..75 -> "pulsing with light in a futuristic server room"
                    sentimentScore in 40..54 -> "floating steadily in a void of static noise"
                    else -> "cracking and dissolving into black sand under heavy rain"
                }

            val style =
                if (isBullish) {
                    if (isAggressive) {
                        "High-octane, neon-drenched cyberpunk, vaporwave colors"
                    } else {
                        "Clean minimalist futuristic, high-end commercial aesthetic"
                    }
                } else {
                    "Blade Runner noir, cinematic gloom, moody lighting with red accents"
                }

            val camera = "Cinematic slow-motion, low-angle tracking shot, 8k resolution"

            return "{Subject: $subject} + {Action: $action} + {Style: $style} + {Camera: $camera}. Max 50 words."
        }
    }
