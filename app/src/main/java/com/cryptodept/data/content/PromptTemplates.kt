package com.cryptodept.data.content

/**
 * Centralized prompt templates for Content Factory V2.
 * Optimized for high-engagement social media output using real market data.
 */
object PromptTemplates {

    fun buildSocialPostPrompt(scope: String, data: String): String {
        return "Act as a Lead Quantitative Strategist and crypto influencer. " +
                "Generate a professional, high-engagement viral technical analysis report for $scope. " +
                "Market Context: $data. " +
                "Include: 1. Catchy Hook, 2. Key Technical Findings (RSI, Trends, Liquidity), 3. Strategic Verdict, 4. 5 Relevant Tags. " +
                "Tone: Cyber-technical, authoritative, and urgency-driven. " +
                "STRICT RULE: Always end the post with this exact link: " +
                "Download CryptoDept Terminal: https://play.google.com/store/apps/details?id=com.cryptodept"
    }

    fun buildInfographicPrompt(scope: String, data: String): String {
        return "Act as a Data Visualization Architect. Design a technical 5-panel infographic for $scope based on this analysis: $data. " +
                "Panel 1: TITLE/PRICE, Panel 2: RSI & TREND STATUS, Panel 3: WHALE FLOW BIAS, Panel 4: RISK SCORE VERDICT, Panel 5: CTA. " +
                "Instructions: Provide exact text for each panel. Keep it monospaced and professional. " +
                "Include the keyword 'technical analysis report' in the output description."
    }

    fun buildCinematicVideoPrompt(scope: String, data: String): String {
        return "Act as a Cinematic Director for AI Video generation. " +
                "Create a professional motion prompt for $scope based on these metrics: $data. " +
                "Visual Scenario: Describe a 10-second ultra-realistic shot of a holographic terminal displaying $scope charts and flow maps. " +
                "Style: Volumetric green/amber lighting, drifting dust particles, 8k resolution, cinematic blur. " +
                "Atmosphere: Tense, high-tech, smart money vibes. " +
                "Ensure the output sounds like a 'technical analysis report' for the AI generator."
    }
}
