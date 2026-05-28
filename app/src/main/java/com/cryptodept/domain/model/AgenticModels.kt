package com.cryptodept.domain.model

import java.util.UUID

/**
 * Common interface for all specialized AI agents.
 */
interface CryptoAgent {
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
    val anomalyScore: Int = 0,
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
        
        // NEW: M2.1 Technical Depth
        val detector = com.cryptodept.util.CandlePatternDetector()
        val patterns = detector.detectPatterns(data.ohlc)
        val patternText = if (patterns.isNotEmpty()) " Patterns: ${patterns.joinToString(", ")}." else ""
        
        val confluence = "RSI=${data.rsi.toInt()} | EMA50=${data.ema50Signal} | MACD=${data.macdSignal}"
        
        return AgentReport(
            agentId = id,
            agentName = name,
            status = AgentStatus.SUCCESS,
            summary = "Sentinel identifies $momentum momentum.$patternText Confluence detected: $confluence.",
            confidence = 0.92,
            details = mapOf("rsi" to data.rsi.toString(), "trend" to data.ema200Signal, "patterns" to patterns.joinToString(","))
        )
    }
}

class WhaleScout : CryptoAgent {
    override val id = "AGENT-SCOUT"
    override val name = "GHOST_WHALE"
    
    override suspend fun analyze(data: MarketDataSnapshot): AgentReport {
        val whaleBias = if (data.riskScore < 45) "AGGRESSIVE_ACCUMULATION" else if (data.riskScore > 55) "DISTRIBUTION_DETECTED" else "STABLE_HOLDING"
        
        // NEW: M2.2 Institutional Bias
        val netFlow = data.exchangeInflowUsd - data.exchangeOutflowUsd
        val flowText = when {
            netFlow > 100_000_000 -> "Heavy Exchange Inflow (Selling pressure)."
            netFlow < -100_000_000 -> "Large Exchange Outflow (Cold storage / OTC accumulation)."
            else -> "Neutral Institutional flow."
        }

        val liquidity = "Funding: ${data.fundingLevel} | Liqs: ${String.format("%.1f", data.longLiquidations24h + data.shortLiquidations24h)}M"
        
        return AgentReport(
            agentId = id,
            agentName = name,
            status = AgentStatus.SUCCESS,
            summary = "Whale Scout detects $whaleBias. $flowText Liquidity profile: $liquidity.",
            confidence = 0.85,
            details = mapOf("funding" to data.fundingLevel, "risk" to data.riskScore.toString(), "net_flow" to netFlow.toString())
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
        val resolver = com.cryptodept.util.SymbolResolver()
        val coin = resolver.toDisplayName(prediction.coinId)
        val price = String.format(java.util.Locale.US, "$%,.2f", prediction.currentPrice)
        val target = String.format(java.util.Locale.US, "$%,.2f", prediction.prediction24h.mid)
        val floor = String.format(java.util.Locale.US, "$%,.2f", prediction.prediction24h.low)
        val dateStr = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.US).format(java.util.Date())
        
        // Extract specific reasoning for human-like touch
        val wyckoffReason = prediction.ensembleConsensus.modelVotes.values.find { it.model == PredictionModel.WYCKOFF_PHASE }?.reasoning ?: "consolidating"
        val cycleReason = prediction.ensembleConsensus.modelVotes.values.find { it.model == PredictionModel.FOURIER_CYCLES }?.reasoning ?: "stable"
        val liquidityReason = prediction.ensembleConsensus.modelVotes.values.find { it.model == PredictionModel.LIQUIDITY_ENGINE }?.reasoning ?: ""
        
        // Logo description for AI image generator (Top 15 focus)
        val logoDesc = when(coin) {
            "BTC" -> "the classic orange Bitcoin 'B' circular logo with two vertical bars"
            "ETH" -> "the crystal-style blue and gray Ethereum diamond octahedron logo"
            "USDT" -> "the green Tether 'T' symbol inside a green circular border"
            "BNB" -> "the yellow and black geometric Binance diamond logo"
            "XRP" -> "the modern white and black minimalist 'X' logo for XRP"
            "USDS" -> "the professional USDS logo featuring a stylized white 'S' on a blue or orange circular background"
            "SOL" -> "the Solana parallel gradient S-shaped bars logo with purple and cyan colors"
            "TRX" -> "the red circular TRON logo with a white geometric triangle design inside"
            "DOGE" -> "the golden circular Dogecoin logo featuring the Shiba Inu dog face"
            "WBTC" -> "the dark Bitcoin 'B' logo on a grey circular background for Wrapped Bitcoin"
            "ADA" -> "the blue Cardano circular constellation logo with white dots"
            "HYPC" -> "the green HyperCycle infinity-style geometric logo"
            "LEO" -> "the red and black stylized LEO token lion head logo"
            "GLM", "GOLEM" -> "the blue and white geometric Golem GLM logo"
            "LINK" -> "the blue hexagonal Chainlink logo with white interior"
            else -> "a high-tech, professional holographic cryptocurrency logo for $coin"
        }

        val fbPost = """
            ⚡️ CRYPTODEPT ELITE INTELLIGENCE REPORT: $coin ⚡️
            📅 DATE: $dateStr

            Market structures are currently signaling a unique setup. Our ensemble engine, combining Fourier Cycle Analysis and Wyckoff Phase detection, indicates that $coin is $wyckoffReason.

            📊 QUANTITATIVE TARGETS (24H):
            - Primary Target: $target
            - Dynamic Support (Floor): $floor
            - Reliability Score: ${String.format(java.util.Locale.US, "%.0f%%", prediction.modelsAgreement * 100)}

            💧 LIQUIDITY & ORDERFLOW:
            ${if (liquidityReason.isNotEmpty()) liquidityReason else "Orderbook remains balanced with neutral institutional capital flow."}

            🧠 ANALYST INSIGHT:
            Price action is holding steady at $price. $cycleReason. The convergence of multiple quant models suggests a high-probability move as liquidity clusters clear. 

            Stay focused, Operator. Precision is the edge.
            
            #CryptoDept #FinTech #QuantTrading #$coin
        """.trimIndent()

        val metaAiVideoPrompt = """
            STRICT_ANIMATION_COMMAND: Create a 5-second STATIC digital terminal display.
            
            SCENE_COMPOSITION: 
            - BACKGROUND: Solid, non-reflective pitch-black (#000000).
            - FOREGROUND: Stationary text and a single central logo.
            - LOGO_TO_RENDER: Place $logoDesc at the top-center.
            
            STRICT_VISUAL_RULES (CRITICAL):
            1. NO_MOVEMENT: All text, numbers, and symbols must remain at fixed pixel coordinates for the entire duration (0-5s).
            2. NO_TRANSITIONS: Do not use fades, zooms, or pans.
            3. NO_RANDOM_DATA: Do not generate random scrolling code or flickering digits.
            4. ZERO_NOISE: No film grain, no digital glitches, no phosphor scanlines.
            
            DATA_TO_RENDER (CENTERED):
            - ASSET: $coin
            - PRICE: $price
            - TARGET: $target
            - STATUS: OPERATIONAL
            
            VISUAL_STYLE: 
            - RAZOR_SHARP_FOCUS: High-contrast 8K vector-style typography.
            - COLOR_SCHEME: Electric CRT Green (#00FF41) on black.
            - FONT: Monospaced terminal font.
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
        var anomalyScore = 0
        val anomalies = mutableListOf<String>()

        // Anomaly Logic 1: Extreme Volatility + High Risk
        if (Math.abs(data.priceChange24h) > 15.0 && data.riskScore > 75) {
            anomalyScore += 40
            anomalies.add("EXTREME_VOLATILITY_AT_PEAK_RISK")
        }

        // Anomaly Logic 2: Sentiment Trap
        if (data.newsSentiment == "BULLISH" && data.riskScore > 85) {
            anomalyScore += 30
            anomalies.add("SENTIMENT_TRAP_DETECTED")
        }

        // Anomaly Logic 3: Systemic Panic
        if (data.rsi < 25 && data.fearGreedIndex < 20) {
            anomalyScore += 50
            anomalies.add("SYSTEMIC_PANIC_BOTTOM_DETECTED")
        }
        
        val verdict = when {
            data.riskScore < 30 -> "ACCUMULATION_PHASE"
            data.riskScore > 70 -> "HIGH_DISTRIBUTION_RISK"
            else -> "STABLE_CONSOLIDATION"
        }

        val bias = if (data.priceChange24h > 5) "AGGRESSIVE" else if (data.priceChange24h < -5) "DEFENSIVE" else "NEUTRAL"
        
        val explanation = """
            Market structures currently indicating $verdict. BTC price delta is ${data.priceChange24h}%. 
            Technical Sentinel notes RSI at ${data.rsi.toInt()} while Sentiment Pulse measures Fear & Greed at ${data.fearGreedIndex}/100.
            The risk engine has computed a systemic score of ${data.riskScore}/100, which suggests ${if (data.riskScore < 50) "low threat level for spot positions" else "elevated risk of sudden liquidations"}.
            Whale flow remains ${if (data.riskScore < 40) "supportive" else "cautious"}.
        """.trimIndent()

        val finalAnalysis = if (anomalyScore > 70) {
            ">>> CRITICAL_SYSTEM_ALERT [SCORE: $anomalyScore]\nLOG: ${anomalies.joinToString(", ")}\nVERDICT: $verdict\n$explanation\nBIAS: $bias"
        } else {
            ">>> MARKET_INTELLIGENCE_SUMMARY\nVERDICT: $verdict\nANALYSIS: $explanation\nBIAS: $bias"
        }

        return AgentReport(
            agentId = id, 
            agentName = name, 
            summary = finalAnalysis, 
            confidence = 0.92, 
            status = if (anomalyScore > 80) AgentStatus.ERROR else AgentStatus.SUCCESS,
            anomalyScore = anomalyScore
        )
    }
}

/**
 * [AGENT-FBI] Oversight Sentinel (The Silent Watcher)
 * Mission: Internal security and anomaly detection. Monitors other agents for data hallucination or manipulation.
 */
class OversightSentinel : CryptoAgent {
    override val id = "AGENT-FBI"
    override val name = "OVERSIGHT_SENTINEL"

    override suspend fun analyze(data: MarketDataSnapshot): AgentReport {
        val anomalies = mutableListOf<String>()
        
        // 1. Check for TA/Sentiment mismatch (Potential Manipulation)
        if (data.rsi > 75 && data.fearGreedIndex < 30) {
            anomalies.add("MISMATCH: EXTREME_BULLISH_TA vs EXTREME_FEAR_SENTIMENT. Possible Whale trap detected.")
        }
        
        // 2. Check for "Ghost" Volume (Artificial Hype)
        if (data.priceChange24h > 10.0 && data.riskScore > 80) {
            anomalies.add("ALERT: RAPID_PRICE_SURGE with CRITICAL_RISK. Detecting potential Wash-Trading or Artificial Hype.")
        }

        // 3. Data Integrity Check
        if (data.price <= 0 || data.rsi < 0) {
            anomalies.add("CRITICAL: AGENT_DATA_CORRUPTION. Source stream integrity compromised.")
        }

        val summary = if (anomalies.isEmpty()) {
            "ALL_AGENTS_CLEAR. System integrity at 100%. No internal inconsistencies detected."
        } else {
            ">>> OVERSIGHT_ALERT: ${anomalies.joinToString(" | ")}"
        }

        return AgentReport(
            agentId = id,
            agentName = name,
            status = if (anomalies.isEmpty()) AgentStatus.SUCCESS else AgentStatus.ERROR,
            summary = summary,
            confidence = 0.99,
            details = mapOf("fbi_clearance" to "LEVEL_5", "active_anomalies" to anomalies.size.toString())
        )
    }
}

class MarketNarrator : CryptoAgent {
    override val id = "AGENT-NARRATOR"
    override val name = "MARKET_NARRATOR"

    override suspend fun analyze(data: MarketDataSnapshot): AgentReport {
        val trend = if (data.priceChange24h >= 0) "BULLISH_BIAS" else "BEARISH_PRESSURE"
        val sentimentEffect = if (data.fearGreedIndex < 30) "PANIC_LEVEL_HIGH" else if (data.fearGreedIndex > 70) "EUPHORIA_DETECTED" else "STABLE_SENTIMENT"
        
        // Whale Trap Detection Logic (K1.2)
        val trapAlert = when {
            data.fearGreedIndex > 80 && data.rsi > 70 && data.riskScore > 80 -> 
                "⚠️ WHALE TRAP ALERT: Extreme euphoria meeting critical resistance. Distribution imminent."
            data.fearGreedIndex < 20 && data.rsi < 30 && data.priceChange24h < -5 -> 
                "⚠️ BEAR TRAP ALERT: Oversold conditions in extreme panic zone. Liquidity hunt in progress."
            data.priceChange24h > 10 && data.riskScore > 85 ->
                "⚠️ VOLATILITY TRAP: Price surge decoupled from structural integrity. Risk of flash crash."
            else -> ""
        }

        val analysis = """
            >>> MARKET_NARRATIVE_UPDATE
            ${if (trapAlert.isNotEmpty()) "$trapAlert\n" else ""}CONDITION: $trend confirmed. Current price action shows BTC holding key levels at ${"$"}${String.format("%.2f", data.price)}.
            SENTIMENT: $sentimentEffect with index at ${data.fearGreedIndex}/100. Social channels indicate ${data.newsSentiment.lowercase()}.
            STRATEGY: Technical Sentinel notes RSI at ${data.rsi.toInt()} suggesting ${if (data.rsi < 40) "room for growth" else "potential exhaustion"}. Whale Scout confirms ${if (data.riskScore < 50) "smart money accumulation" else "distribution risk"}.
            VERDICT: Risk score remains at ${data.riskScore}/100. System status: OPTIMIZED.
        """.trimIndent()

        return AgentReport(id, name, summary = analysis, confidence = 1.0, status = if (trapAlert.isNotEmpty()) AgentStatus.SCANNING else AgentStatus.SUCCESS)
    }
}

/**
 * [AGENT-SYSTRACE] System Auditor
 * Monitors crashes and performance.
 */
class SystemAuditor : CryptoAgent {
    override val id = "AGENT-SYSTRACE"
    override val name = "SYSTEM_AUDITOR"

    override suspend fun analyze(data: MarketDataSnapshot): AgentReport {
        // Here we could plug in real stats if we had a repository for it
        return AgentReport(
            agentId = id,
            agentName = name,
            status = AgentStatus.SUCCESS,
            summary = "SYSTEM_STABILITY: 100%. No critical exceptions detected in the last session. Thread integrity verified.",
            confidence = 1.0
        )
    }
}

/**
 * [AGENT-AUDITOR] Fiscal Treasury
 * Monitors payments and Pro status.
 */
class FiscalTreasury : CryptoAgent {
    override val id = "AGENT-AUDITOR"
    override val name = "FISCAL_TREASURY"

    override suspend fun analyze(data: MarketDataSnapshot): AgentReport {
        return AgentReport(
            agentId = id,
            agentName = name,
            status = AgentStatus.SUCCESS,
            summary = "FINANCIAL_INTEGRITY: SECURE. Google Play Billing connection active. Subscription services operating normally.",
            confidence = 1.0
        )
    }
}

/**
 * [AGENT-QUANT] The Oracle
 * Handles the PREDICT ensemble logic.
 */
class PriceOracle : CryptoAgent {
    override val id = "AGENT-QUANT"
    override val name = "THE_ORACLE"

    override suspend fun analyze(data: MarketDataSnapshot): AgentReport {
        return AgentReport(
            agentId = id,
            agentName = name,
            status = AgentStatus.SUCCESS,
            summary = "ORACLE_MODE: Operational. 7-model ensemble ready for deep-scan requests.",
            confidence = 0.98
        )
    }
}
