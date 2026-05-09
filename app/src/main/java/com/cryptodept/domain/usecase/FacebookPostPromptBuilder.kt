package com.cryptodept.domain.usecase

import javax.inject.Inject

/**
 * Generates prompts for AI to create Facebook posts targeted at a global audience.
 */
class FacebookPostPromptBuilder
    @Inject
    constructor() {
        fun build(
            topic: String,
            dataContext: String,
        ): String =
            """
            Write a Facebook post in English targeted at professional investors and enthusiasts.
            
            GUIDELINES:
            - TONE: Professional, analytical, accessible. Avoid hype.
            - FORMAT: Start with a thought-provoking question, then provide an answer based on the context.
            - EMOJI: Use exactly one relevant emoji at the very beginning of the post.
            - LENGTH: 80-120 words.
            - CTA: End with the specific phrase: "Share your thoughts in the comments".
            
            CONTEXT:
            Topic: $topic
            Key Data: $dataContext
            
            GOAL:
            Create engagement through informative, balanced content that feels reliable.
            """.trimIndent()
    }
