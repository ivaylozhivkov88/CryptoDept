package com.cryptodept.domain.model

import java.util.UUID

/**
 * Common interface for all specialized AI agents.
 */
sealed interface CryptoAgent {
    val id: String
    val name: String
    
    suspend fun analyze(data: MarketDataSnapshot): AgentReport
    suspend fun generateMarketingPackage(prediction: PricePrediction): AgentReport = AgentReport("VOID", "VOID", summary = "NO_OP", confidence = 0.0)
}

/**
 * Data models for agent communication.
 */
data class AgentReport(
    val agentId: String,
    val agentName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: AgentStatus = AgentStatus.READY,
    val summary: String,
    val confidence: Double,
    val details: Map<String, String> = emptyMap()
)

enum class AgentStatus {
    READY, SCANNING, SUCCESS, ERROR
}

/**
 * Specific Agent Definitions
 */
class TechnicalSentinel : CryptoAgent {
    override val id = "AGENT-SENTINEL"
    override val name = "TECHNICAL_SENTINEL"
    
    override suspend fun analyze(data: MarketDataSnapshot): AgentReport {
        val momentum = if (data.rsi < 40) "BULLISH_RECOVERY" else if (data.rsi > 60) "BEARISH_EXHAUSTION" else "NEUTRAL"
        val confluence = "RSI=${data.rsi.toInt()} | EMA50=${data.ema50Signal} | MACD=${data.macdSignal}"
        
        return AgentReport(
            agentId = id,
            agentName = name,
            status = AgentStatus.SUCCESS,
            summary = "Sentinel identifies $momentum momentum. Confluence detected: $confluence.",
            confidence = 0.92,
            details = mapOf("rsi" to data.rsi.toString(), "trend" to data.ema200Signal)
        )
    }
}

class WhaleScout : CryptoAgent {
    override val id = "AGENT-SCOUT"
    override val name = "GHOST_WHALE"
    
    override suspend fun analyze(data: MarketDataSnapshot): AgentReport {
        val whaleBias = if (data.riskScore < 45) "AGGRESSIVE_ACCUMULATION" else if (data.riskScore > 55) "DISTRIBUTION_DETECTED" else "STABLE_HOLDING"
        val liquidity = "Funding: ${data.fundingLevel} | Liqs: ${String.format("%.1f", data.longLiquidations24h + data.shortLiquidations24h)}M"
        
        return AgentReport(
            agentId = id,
            agentName = name,
            status = AgentStatus.SUCCESS,
            summary = "Whale Scout detects $whaleBias. Liquidity profile: $liquidity.",
            confidence = 0.85,
            details = mapOf("funding" to data.fundingLevel, "risk" to data.riskScore.toString())
        )
    }
}

class SentimentPulse : CryptoAgent {
    override val id = "AGENT-PULSE"
    override val name = "SENTIMENT_PULSE"
    
    override suspend fun analyze(data: MarketDataSnapshot): AgentReport {
        val pulse = if (data.fearGreedIndex < 35) "EXTREME_FEAR" else if (data.fearGreedIndex > 65) "EUPHORIA" else "RATIONAL"
        val social = "Index: ${data.fearGreedIndex} | News: ${data.newsSentiment}"
        
        return AgentReport(
            agentId = id,
            agentName = name,
            status = AgentStatus.SUCCESS,
            summary = "Sentiment Pulse measures $pulse state. Social indicators: $social.",
            confidence = 0.78,
            details = mapOf("fg_index" to data.fearGreedIndex.toString(), "news" to data.newsSentiment)
        )
    }
}

class MarketingStrategist : CryptoAgent {
    override val id = "AGENT-MARKET"
    override val name = "MARKETING_STRATEGIST"

    override suspend fun analyze(data: MarketDataSnapshot): AgentReport {
        return AgentReport(id, name, summary = "USE_GENERATE_MARKETING_PACKAGE", confidence = 0.0)
    }

    override suspend fun generateMarketingPackage(prediction: PricePrediction): AgentReport {
        val coin = prediction.coinId.uppercase()
        val price = String.format(java.util.Locale.US, "$%,.2f", prediction.currentPrice)
        val target = String.format(java.util.Locale.US, "$%,.2f", prediction.prediction24h.mid)
        val floor = String.format(java.util.Locale.US, "$%,.2f", prediction.prediction24h.low)
        val dateStr = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US).format(java.util.Date())
        
        // Logo description for AI image generator
        val logoDesc = when(coin) {
            "BTC", "BITCOIN" -> "Large orange Bitcoin 'B' circular logo"
            "ETH", "ETHEREUM" -> "Large blue Ethereum crystal diamond logo"
            "XRP", "RIPPLE" -> "Large white Ripple 'X' logo"
            "SOL", "SOLANA" -> "Large Solana S-shaped gradient logo"
            "TRX", "TRON" -> "Large red TRON geometric logo"
            else -> "Large professional crypto currency logo for $coin"
        }
        
        // Extract specific reasoning for human-like touch
        val wyckoffReason = prediction.ensembleConsensus.modelVotes.values.find { it.model == PredictionModel.WYCKOFF_PHASE }?.reasoning ?: "consolidating"
        val cycleReason = prediction.ensembleConsensus.modelVotes.values.find { it.model == PredictionModel.FOURIER_CYCLES }?.reasoning ?: "stable"
        
        val fbPost = """
            ⚡️ CRYPTODEPT ELITE INTELLIGENCE REPORT: $coin ⚡️
            📅 DATE: $dateStr

            Market structures are currently signaling a unique setup. Our ensemble engine, combining Fourier Cycle Analysis and Wyckoff Phase detection, indicates that $coin is $wyckoffReason.

            📊 QUANTITATIVE TARGETS (24H):
            - Primary Target: $target
            - Dynamic Support (Floor): $floor
            - Reliability Score: ${String.format(java.util.Locale.US, "%.0f%%", prediction.modelsAgreement * 100)}

            🧠 ANALYST INSIGHT:
            Price action is holding steady at $price. $cycleReason. The convergence of multiple quant models suggests a high-probability move as liquidity clusters clear. 

            Stay focused, Operator. Precision is the edge.
            
            #CryptoDept #FinTech #QuantTrading #$coin
        """.trimIndent()

        val metaAiVideoPrompt = """
            STRICT_COMMAND: Create a 5-second STILL digital dashboard image. 
            SCENE: Professional high-contrast terminal screen on a solid pitch-black background.
            
            VISUAL_TOP_ELEMENT: $logoDesc centered at the top.
            
            TEXT_DATA_TO_RENDER (EXACTLY AS WRITTEN):
            DATE: $dateStr
            ASSET: $coin
            PRICE: $price
            TARGET: $target
            STATUS: SYSTEM_OPTIMIZED
            
            TECHNICAL_CONSTRAINTS: 
            1. ZERO_MOVEMENT: Do not animate text, background, or camera.
            2. ZERO_FLICKER: No digital glitches, no noise, no pulsing effects.
            3. STABILITY_FIRST: All characters must remain in fixed coordinates for the entire duration.
            
            VISUAL_STYLE: Flat 2D vector-style electric green typography (#00FF41). Razor-sharp 8K focus. No 3D depth, no reflections.
        """.trimIndent()

        return AgentReport(
            id, name, 
            summary = "ELITE_MARKETING_PACKAGE_READY", 
            confidence = prediction.modelsAgreement.toDouble(),
            details = mapOf("facebook_post" to fbPost, "video_prompt" to metaAiVideoPrompt)
        )
    }
}

class NarrativeOrchestrator : CryptoAgent {
    override val id = "AGENT-CORE"
    override val name = "ORCHESTRATOR"
    
    override suspend fun analyze(data: MarketDataSnapshot): AgentReport {
        val verdict = when {
            data.riskScore < 30 -> "ACCUMULATION_PHASE_ACTIVE"
            data.riskScore > 70 -> "DISTRIBUTION_RISK_HIGH"
            else -> "SIDEWAYS_CONSOLIDATION"
        }
        return AgentReport(id, name, summary = "FINAL_VERDICT: $verdict", confidence = 0.88)
    }
}

class MarketNarrator : CryptoAgent {
    override val id = "AGENT-NARRATOR"
    override val name = "MARKET_NARRATOR"

    override suspend fun analyze(data: MarketDataSnapshot): AgentReport {
        val trend = if (data.priceChange24h >= 0) "BULLISH_BIAS" else "BEARISH_PRESSURE"
        val sentimentEffect = if (data.fearGreedIndex < 30) "PANIC_LEVEL_HIGH" else if (data.fearGreedIndex > 70) "EUPHORIA_DETECTED" else "STABLE_SENTIMENT"
        
        val analysis = """
            >>> MARKET_NARRATIVE_UPDATE
            CONDITION: $trend confirmed. Current price action shows BTC holding key levels at ${"$"}${String.format("%.2f", data.price)}.
            SENTIMENT: $sentimentEffect with index at ${data.fearGreedIndex}/100. Social channels indicate ${data.newsSentiment.lowercase()}.
            STRATEGY: Technical Sentinel notes RSI at ${data.rsi.toInt()} suggesting ${if (data.rsi < 40) "room for growth" else "potential exhaustion"}. Whale Scout confirms ${if (data.riskScore < 50) "smart money accumulation" else "distribution risk"}.
            VERDICT: Risk score remains at ${data.riskScore}/100. System status: OPTIMIZED.
        """.trimIndent()

        return AgentReport(id, name, summary = analysis, confidence = 1.0, status = AgentStatus.SUCCESS)
    }
}
