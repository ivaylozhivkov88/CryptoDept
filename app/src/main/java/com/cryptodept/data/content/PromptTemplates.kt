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
        return "Act as an Elite Brand Architect for CryptoDept. Design a high-impact 'Breaking News' style visual for $scope. " +
                "Market Data: $data. " +
                "Visual Layout: Large bold typography saying 'BREAKING: $scope ALPHA SCAN'. " +
                "Style: Intense, high-contrast, professional infographic. Use sharp vector icons (Rockets, Arrows, Charts). " +
                "Background: Dark futuristic skyline with glowing cyber-green highlights. " +
                "Instructions: Describe a visual that looks like an institutional trading news blast. High resolution 8k."
    }

    fun buildCinematicVideoPrompt(scope: String, data: String): String {
        return "Act as a Cinematic Director. Imagine a signature CryptoDept lifestyle POV: " +
                "A first-person view of a professional trader's desk. " +
                "Include: A luxury car key, a high-end watch, and a laptop displaying $scope quantitative metrics ($data). " +
                "Atmosphere: Successful, calm, elite wealth. " +
                "Prompt: Describe a photorealistic 8k cinematic shot with soft morning lighting and sharp UI data."
    }
}
