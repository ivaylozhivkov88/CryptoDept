package com.cryptodept.util

import com.cryptodept.data.datastore.PreferencesService
import com.cryptodept.domain.model.*
import com.cryptodept.domain.usecase.TechnicalAnalysisEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides demonstration data for the Tutorial Tour.
 * 
 * Activated ONLY during onboarding tour, deactivated immediately after.
 */
@Singleton
class DemoModeProvider @Inject constructor(
    private val prefs: PreferencesService,
) {
    private val _demoActive = MutableStateFlow(false)
    val demoActiveState: StateFlow<Boolean> = _demoActive.asStateFlow()
    
    fun isActive(): Boolean = _demoActive.value
    
    fun activate() {
        _demoActive.value = true
    }
    
    fun deactivate() {
        _demoActive.value = false
    }
    
    // ========================================================
    // DASHBOARD DATA
    // ========================================================
    
    fun getDemoPriceTickers(): List<DemoTicker> = listOf(
        DemoTicker("BTC", 103_245.50, 2.34),
        DemoTicker("ETH", 3_425.80, 1.85),
        DemoTicker("SOL", 212.45, -0.52),
        DemoTicker("BNB", 645.20, 0.87),
        DemoTicker("XRP", 0.5845, 1.20),
    )
    
    fun getDemoAiNarrative(): String = """
        BTC defends critical ${'$'}103k support. Ensemble confidence: 73%.
        ETH funding rates flipped negative — leveraged shorts at risk.
        Altcoin dominance up 2.3%, suggesting capital rotation incoming.
        Strategic bias: cautious-bullish until ${'$'}105k breakout confirms.
    """.trimIndent()

    fun getDemoAgentStatuses(): Map<String, AgentStatus> = mapOf(
        "AGENT-SENTINEL" to AgentStatus.SUCCESS,
        "AGENT-PULSE" to AgentStatus.SUCCESS,
        "AGENT-SYSTRACE" to AgentStatus.SUCCESS,
        "AGENT-QUANT" to AgentStatus.SUCCESS,
        "AGENT-AUDITOR" to AgentStatus.SUCCESS
    )

    fun getDemoEvents(): List<SystemEvent> = listOf(
        SystemEvent(type = EventType.SYSTEM_STATUS, message = "TERMINAL_BOOT: ENSEMBLE ENGINE v2.1 ONLINE"),
        SystemEvent(type = EventType.NETWORK_HEALTH, message = "NETWORK_SCAN: ALL API SOURCES OPERATIONAL"),
        SystemEvent(type = EventType.TECHNICAL_LEVEL, message = "THRESHOLD_MET: BTC TESTING ${'$'}103,000 SUPPORT"),
        SystemEvent(type = EventType.MARKET_SIGNAL, message = "AI_SIGNAL: BULLISH DIVERGENCE DETECTED ON ETH/USD"),
        SystemEvent(type = EventType.SYSTEM_STATUS, message = "ORCHESTRATOR: GENERATING STRATEGIC NARRATIVE...")
    )
    
    fun getDemoSentiment(): DemoSentiment = DemoSentiment(
        fearGreedIndex = 62,
        fearGreedLabel = "GREED",
        redditPositive = 68,
        cryptoPanicScore = 7.2,
    )
    
    fun getDemoNetworkHealth(): DemoNetworkHealth = DemoNetworkHealth(
        btcGasFeeSat = 12,
        ethGasFeeGwei = 24,
        solCongestion = "LOW",
        mempoolBacklog = 8420,
    )
    
    // ========================================================
    // MARKETS DATA
    // ========================================================

    fun getDemoMarketsList(): List<DemoMarketCoin> = listOf(
        DemoMarketCoin("BTC", "Bitcoin", 103_245.50, 2.34, 2_034_500_000_000L, 1),
        DemoMarketCoin("ETH", "Ethereum", 3_425.80, 1.85, 412_700_000_000L, 2),
        DemoMarketCoin("USDT", "Tether", 1.0001, 0.01, 121_300_000_000L, 3),
        DemoMarketCoin("BNB", "BNB", 645.20, 0.87, 93_400_000_000L, 4),
        DemoMarketCoin("SOL", "Solana", 212.45, -0.52, 99_800_000_000L, 5),
        DemoMarketCoin("XRP", "XRP", 0.5845, 1.20, 32_100_000_000L, 6),
        DemoMarketCoin("USDC", "USD Coin", 1.0000, 0.00, 33_200_000_000L, 7),
        DemoMarketCoin("ADA", "Cardano", 0.4523, 0.95, 16_100_000_000L, 8),
        DemoMarketCoin("AVAX", "Avalanche", 38.45, -4.21, 15_700_000_000L, 9),
        DemoMarketCoin("DOGE", "Dogecoin", 0.387, 5.67, 56_200_000_000L, 10),
        DemoMarketCoin("TRX", "TRON", 0.245, 0.32, 21_300_000_000L, 11),
        DemoMarketCoin("LINK", "Chainlink", 18.67, -3.85, 11_500_000_000L, 12),
        DemoMarketCoin("DOT", "Polkadot", 8.45, -1.20, 12_400_000_000L, 13),
        DemoMarketCoin("MATIC", "Polygon", 0.834, 1.45, 8_300_000_000L, 14),
        DemoMarketCoin("LTC", "Litecoin", 95.23, 0.45, 7_200_000_000L, 15),
    )

    fun getDemoGlobalStats(): DemoGlobalStats = DemoGlobalStats(
        totalMarketCap = 3_120_000_000_000L,
        btcDominance = 58.1,
        ethDominance = 12.7,
        volume24h = 187_400_000_000L,
    )

    fun getDemoTrackedCoins(): List<String> = listOf("BTC", "ETH", "SOL", "BNB", "XRP")
    
    // ========================================================
    // ANALYSIS DATA
    // ========================================================
    
    fun getDemoAnalysis(): DemoAnalysis = DemoAnalysis(
        coinId = "bitcoin",
        coinSymbol = "BTC",
        currentPrice = 103_245.50,
        rsi = 58.4,
        rsiLabel = "NEUTRAL",
        macdValue = 245.8,
        macdLabel = "BULLISH_CROSS",
        ema50 = 101_200.0,
        ema200 = 95_400.0,
        verdict = "Cautiously bullish. Support holding, momentum building.",
        confidence = 0.73f,
    )

    fun getDemoOhlc(): List<OHLCData> {
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L
        return listOf(
            OHLCData(now - 5 * dayMs, 98000.0, 101000.0, 97500.0, 100500.0, 500.0),
            OHLCData(now - 4 * dayMs, 100500.0, 102000.0, 99800.0, 101200.0, 450.0),
            OHLCData(now - 3 * dayMs, 101200.0, 104500.0, 101000.0, 103800.0, 600.0),
            OHLCData(now - 2 * dayMs, 103800.0, 105200.0, 102500.0, 102900.0, 550.0),
            OHLCData(now - 1 * dayMs, 102900.0, 104100.0, 102800.0, 103245.5, 480.0)
        )
    }

    fun getDemoPatterns(): List<TechnicalAnalysisEngine.PatternDetection> = listOf(
        TechnicalAnalysisEngine.PatternDetection(
            pattern = TechnicalAnalysisEngine.CandlePattern.HAMMER,
            description = "Bullish hammer detected on 4H timeframe.",
            isBullish = true
        )
    )

    fun getDemoTraces(): List<AnalysisTrace> = listOf(
        AnalysisTrace("RSI", 60, "NEUTRAL", "RSI at 58.4 shows healthy momentum without being overbought."),
        AnalysisTrace("MACD", 85, "BULLISH_CROSS", "Recent MACD crossover confirms upward trend."),
        AnalysisTrace("EMA", 75, "SUPPORT", "Price bouncing off EMA 50 support level.")
    )
    
    // ========================================================
    // PREDICTIONS
    // ========================================================
    
    fun getDemoPredictions(): DemoPredictions = DemoPredictions(
        fftVote = "UP",
        monteCarloVote = "UP",
        wyckoffVote = "NEUTRAL",
        elliottVote = "UP",
        hurstVote = "UP",
        linearVote = "DOWN",
        ensembleAgreement = 4,
        totalModels = 6,
        targetHigh = 108_500.0,
        targetLow = 98_500.0,
    )
}

// ========================================================
// DATA CLASSES
// ========================================================

data class DemoTicker(
    val symbol: String,
    val price: Double,
    val change24h: Double,
)

data class DemoMover(
    val symbol: String,
    val price: Double,
    val change24h: Double,
    val isGainer: Boolean,
)

data class DemoSentiment(
    val fearGreedIndex: Int,
    val fearGreedLabel: String,
    val redditPositive: Int,
    val cryptoPanicScore: Double,
)

data class DemoNetworkHealth(
    val btcGasFeeSat: Int,
    val ethGasFeeGwei: Int,
    val solCongestion: String,
    val mempoolBacklog: Int,
)

data class DemoMarketCoin(
    val symbol: String,
    val name: String,
    val price: Double,
    val change24h: Double,
    val marketCap: Long,
    val rank: Int,
)

data class DemoGlobalStats(
    val totalMarketCap: Long,
    val btcDominance: Double,
    val ethDominance: Double,
    val volume24h: Long,
)

data class DemoAnalysis(
    val coinId: String,
    val coinSymbol: String,
    val currentPrice: Double,
    val rsi: Double,
    val rsiLabel: String,
    val macdValue: Double,
    val macdLabel: String,
    val ema50: Double,
    val ema200: Double,
    val verdict: String,
    val confidence: Float,
)

data class DemoPredictions(
    val fftVote: String,
    val monteCarloVote: String,
    val wyckoffVote: String,
    val elliottVote: String,
    val hurstVote: String,
    val linearVote: String,
    val ensembleAgreement: Int,
    val totalModels: Int,
    val targetHigh: Double,
    val targetLow: Double,
)
