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
            emit(">>> QUANT_INFOGRAPHIC_SPEC [$scope]\n\n")
            emit("[PANEL 1] TITLE: $scope ALPHA SCAN\n")
            emit("[PANEL 2] PRICE: ${dataStr.substringBefore(",")} | STATUS: ACTIVE\n")
            emit("[PANEL 3] INDICATORS: ${dataStr.substringAfter(",").trim()}\n")
            emit("[PANEL 4] WHALE_FLOW: NET_POSITIVE (ACCUMULATION)\n")
            emit("[PANEL 5] ACTION: SCAN FOR MORE -> @CRYPTODEPT")
        } else if (isVideo) {
            emit(">>> CINEMATIC_MOTION_PROMPT [$scope]\n\n")
            emit("ULTRA-REALISTIC 8K CINEMATIC SHOT: A floating 3D holographic interface displaying $scope market data. ")
            emit("Green phosphor text pulses softly in a pitch-black terminal environment (#000000). ")
            emit("Data nodes drifting: $dataStr. ")
            emit("Lighting: Volumetric rays from a central Bitcoin logo, drifting particles in blue light. ")
            emit("Razor-sharp vector typography, high contrast, smart money atmosphere. No glitches. Stationary text.")
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
