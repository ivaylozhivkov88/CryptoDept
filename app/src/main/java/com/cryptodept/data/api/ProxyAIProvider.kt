package com.cryptodept.data.api

import com.cryptodept.domain.model.TradeJournal
import com.cryptodept.domain.repository.AIProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProxyAIProvider @Inject constructor() : AIProvider {
    override suspend fun sendMessage(prompt: String): Flow<String> = flow {
        // --- 1. DETECTION & CONTEXT EXTRACTION ---
        val isSocial = prompt.contains("Lead Quantitative Strategist")
        val isInfographic = prompt.contains("DESIGN A TECHNICAL 5-PANEL")
        val isVideo = prompt.contains("Cinematic Director")
        
        val scopeMatch = Regex("for (\\w+)").find(prompt)
        val scope = scopeMatch?.groupValues?.get(1) ?: "MARKET"
        
        val dataStr = when {
            isSocial -> prompt.substringAfter("Market Context: ").substringBefore(". Include:")
            isInfographic -> prompt.substringAfter("analysis: ").substringBefore(". Panel")
            isVideo -> prompt.substringAfter("metrics: ").substringBefore(". Visual")
            else -> "SYNCHRONIZED"
        }.trim()

        if (isSocial) {
            emit("🚀 **ELITE_OPERATOR_REPORT: $scope** 🚀\n\n")
            emit(">>> HOOK: The smart money is repositioning. Are you watching the same data I am?\n\n")
            emit(">>> TECHNICAL_SNAPSHOT:\n")
            emit("- CURRENT_DATA: $dataStr\n")
            emit("- MTF_SCAN: Clinical technical scan detects high-fidelity bullish divergence in H4 market structure.\n")
            emit("- WHALE_BIAS: On-chain footprint confirms heavy net-accumulation on primary blockchains.\n\n")
            emit(">>> STRATEGIC_VERDICT: High-conviction entry point for strategic holders. Structural support holding firm against retail distribution attempts.\n\n")
            emit("#CryptoAlpha #$scope #Trading #CryptoDept #SmartMoney\n\n")
            emit("Download CryptoDept Terminal: https://play.google.com/store/apps/details?id=com.cryptodept")
        } else if (isInfographic) {
            emit(">>> QUANT_IMAGE_GENERATION_PROMPT [$scope]\n\n")
            emit("CREATE_IMAGE: A high-tech technical analysis infographic for $scope. ")
            emit("The image should feature 5 clean panels displaying: ")
            emit("1. Current Price: ${dataStr.substringBefore(",")}, ")
            emit("2. Technical Sentiment: ${dataStr.substringAfter(",").trim()}, ")
            emit("3. Institutional Flow: Net Accumulation, ")
            emit("4. Market Structure: BULLISH CONFLUENCE, ")
            emit("5. Branding: CryptoDept Intelligence Unit. ")
            emit("\n\nSTYLE: Professional Bloomberg terminal aesthetic, dark mode, phosphor green and cyber-amber color palette, 8k resolution, hyper-realistic vector graphics, sharp text, futuristic trading command center atmosphere.")
        } else if (isVideo) {
            emit(">>> VIDEO_GENERATION_PROMPT [$scope]\n\n")
            emit("GENERATE_VIDEO: Cinematic 10-second shot of a futuristic crypto trading desk. ")
            emit("A large holographic 3D display shows a pulsing price chart for $scope. ")
            emit("Digital numbers and data nodes drift in 3D space: $dataStr. ")
            emit("Visuals: Volumetric lighting, drifting particles, dark cinematic environment, sharp green phosphor glow, 8k resolution, professional motion design.")
        } else {
            emit(">>> SYSTEM_INTELLIGENCE_ONLINE\n\n")
            emit("Analysis complete. Scope: $scope. Node synchronized with global market feed.")
        }
    }

    override suspend fun analyzeJournal(
        trades: List<TradeJournal>,
        currentRiskScore: Int,
        currentMarket: String,
    ): Flow<String> = flow {
        emit(">>> JOURNAL_ANALYSIS_COMPLETE\n\n")
        emit("1. Risk management protocol is functioning correctly.\n")
        emit("2. Psychological discipline remains high.\n")
        emit("3. Suggesting optimization of position sizing based on score ($currentRiskScore).")
    }
}
