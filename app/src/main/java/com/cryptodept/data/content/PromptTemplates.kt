package com.cryptodept.data.content

import com.cryptodept.domain.model.AudienceProfile
import com.cryptodept.viewmodel.ContentTemplate

/**
 * Centralized prompt templates for Content Studio V2.
 */
object PromptTemplates {

    fun buildPrompt(
        template: ContentTemplate,
        audience: AudienceProfile,
        params: Map<String, Any>
    ): String {
        val audienceTone = when (audience) {
            AudienceProfile.DAY_TRADER -> "aggressive, technical, focused on volatility and immediate R:R."
            AudienceProfile.HODLER -> "calm, fundamental, focused on macro cycles and institutional accumulation."
            AudienceProfile.DEFI_USER -> "high-energy, risk-tolerant, focused on yields, TVL, and early-stage opportunities."
            AudienceProfile.CRYPTO_NEWBIE -> "educational, clear, avoiding jargon, focused on risk safety and basic concepts."
            AudienceProfile.CONTENT_FAN -> "entertaining, viral, using hooks and curiosity-driven language."
        }

        val baseContext = "Act as a professional marketing strategist for the 'CryptoDept Elite' financial terminal. " +
                "Target audience tone is $audienceTone\n\n"

        return when (template) {
            ContentTemplate.DAILY_RECAP -> buildDailyRecap(baseContext, params)
            ContentTemplate.VIDEO_PROMPT -> buildVideoPrompt(baseContext, params)
            ContentTemplate.THUMBNAIL -> buildThumbnailPrompt(baseContext, params)
            ContentTemplate.FACEBOOK_POST -> buildFacebookPrompt(baseContext, params)
            ContentTemplate.WHALE_NARRATOR -> buildWhalePrompt(baseContext, params)
            ContentTemplate.WHAT_IF -> buildWhatIfPrompt(baseContext, params)
            ContentTemplate.COMPARISON -> buildComparisonPrompt(baseContext, params)
            ContentTemplate.NEWSLETTER -> buildNewsletterPrompt(baseContext, params)
        }
    }

    private fun buildDailyRecap(base: String, params: Map<String, Any>): String {
        val asset = params["asset"] ?: "BTC"
        return "$base Generate a daily market recap focusing on $asset. Mention Fear & Greed index, top movers, and a technical verdict."
    }

    private fun buildVideoPrompt(base: String, params: Map<String, Any>): String {
        val asset = params["asset"] ?: "BTC"
        return "$base Generate a cinematic, high-retention 60-second video script for $asset. " +
                "STRICT RULES: DO NOT include technical status lines like 'STATUS: OPERATIONAL'. " +
                "DO NOT use flickering digits, scrolling numbers, or changing price digits in descriptions—use static symbols or metaphors instead (e.g., 'a glowing neon ticker frozen in time'). " +
                "Style: Cyberpunk Noir / High-Tech Minimalist. " +
                "Scenes: 1. Hook (Dramatic macro shot), 2. The Problem (Market noise), 3. The Solution (CryptoDept Alpha), 4. Evidence (On-chain moves), 5. CTA. " +
                "Include visual descriptions that are AI-video generator friendly (no temporal inconsistencies)."
    }

    private fun buildThumbnailPrompt(base: String, params: Map<String, Any>): String {
        val headline = params["headline"] ?: "CRYPTO SHOCK"
        return "$base Suggest 3 high-CTR thumbnail concepts and primary text overlays for a video titled '$headline'."
    }

    private fun buildFacebookPrompt(base: String, params: Map<String, Any>): String {
        val topic = params["topic"] ?: "Market Update"
        return "$base Write a engaging Facebook post about $topic. Use relevant emojis and clear calls to action."
    }

    private fun buildWhalePrompt(base: String, params: Map<String, Any>): String {
        return "$base Generate a dramatic whale alert script based on recent $500k+ on-chain movements. Explain the likely intent (dump vs accumulation)."
    }

    private fun buildWhatIfPrompt(base: String, params: Map<String, Any>): String {
        val asset = params["asset"] ?: "BTC"
        val price = params["price"] ?: "$100,000"
        return "$base Create a 'What If' scenario for $asset hitting $price. Discuss market implications and sentiment shift."
    }

    private fun buildComparisonPrompt(base: String, params: Map<String, Any>): String {
        val coinA = params["coinA"] ?: "ETH"
        val coinB = params["coinB"] ?: "SOL"
        return "$base Compare $coinA vs $coinB based on current technical structure and recent network activity."
    }

    private fun buildNewsletterPrompt(base: String, params: Map<String, Any>): String {
        return "$base Draft a weekly newsletter digest highlighting the most critical alpha discovered by CryptoDept agents this week."
    }
}
