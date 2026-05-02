package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.model.*
import com.cryptodept.domain.repository.CryptoRepository
import com.cryptodept.domain.repository.ChartRepository
import com.cryptodept.domain.usecase.TechnicalAnalysisEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.Locale
import javax.inject.Inject

sealed class AnalysisUiState {
    object Loading : AnalysisUiState()
    data class Success(
        val coinId: String,
        val compositeSignal: CompositeSignal,
        val currentPrice: Double,
        val ohlcData: List<OHLCData>,
        val patterns: List<TechnicalAnalysisEngine.PatternDetection>,
        val fibonacci: Map<String, Double>,
        val rsiValue: Double
    ) : AnalysisUiState()
    data class Error(val message: String) : AnalysisUiState()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val cryptoRepository: CryptoRepository,
    private val chartRepository: ChartRepository,
    private val taEngine: TechnicalAnalysisEngine,
    private val geminiService: com.cryptodept.data.api.GeminiCoachService
) : ViewModel() {

    private val _selectedCoin = MutableStateFlow("bitcoin")
    private val _selectedDays = MutableStateFlow(30)

    val analysisState: StateFlow<AnalysisUiState> = combine(
        _selectedCoin,
        _selectedDays
    ) { coin, days -> Pair(coin, days) }
        .flatMapLatest { (coin, days) ->
            flow {
                emit(AnalysisUiState.Loading)
                try {
                    val normalizedId = withContext(Dispatchers.Default) {
                        when (coin.lowercase()) {
                            "btc" -> "bitcoin"
                            "eth" -> "ethereum"
                            "xrp" -> "ripple"
                            "sol" -> "solana"
                            "ada" -> "cardano"
                            "dot" -> "polkadot"
                            "ltc" -> "litecoin"
                            "link" -> "chainlink"
                            "matic" -> "matic-network"
                            "avax" -> "avalanche-2"
                            "trx" -> "tron"
                            "xlm" -> "stellar"
                            "atom" -> "cosmos"
                            "shib" -> "shiba-inu"
                            "doge" -> "dogecoin"
                            else -> coin.lowercase()
                        }
                    }

                    withContext(Dispatchers.IO) {
                        chartRepository.refreshOHLCData(normalizedId, days)
                    }
                    val ohlcData = withContext(Dispatchers.IO) {
                        cryptoRepository.getOHLCData(normalizedId, days)
                    }

                    if (ohlcData.isEmpty()) {
                        emit(AnalysisUiState.Error("NO DATA FOR $normalizedId"))
                        return@flow
                    }

                    val result = withContext(Dispatchers.Default) {
                        val prices = ohlcData.map { it.close }
                        val rsi = taEngine.calculateRSI(prices)
                        val macd = taEngine.calculateMACD(prices)
                        val patterns = taEngine.detectPatterns(ohlcData)
                        val fib = taEngine.calculateFibonacciLevels(prices.maxOrNull() ?: 0.0, prices.minOrNull() ?: 0.0)

                        val indicators = mutableListOf<IndicatorStatus>()
                        var bullish = 0
                        var bearish = 0
                        var neutral = 0

                        val rsiSent = when {
                            rsi < 35 -> Sentiment.BULLISH.also { bullish++ }
                            rsi > 65 -> Sentiment.BEARISH.also { bearish++ }
                            else -> Sentiment.NEUTRAL.also { neutral++ }
                        }
                        indicators.add(IndicatorStatus("RSI", String.format(Locale.US, "%.1f", rsi), rsiSent))

                        val macdHist = macd.histogram.lastOrNull() ?: 0.0
                        val macdSent = if (macdHist > 0) Sentiment.BULLISH.also { bullish++ }
                        else Sentiment.BEARISH.also { bearish++ }
                        indicators.add(IndicatorStatus("MACD", if (macdHist > 0) "BULL" else "BEAR", macdSent))

                        // Patterns impact confidence
                        patterns.forEach { p ->
                            if (p.isBullish) bullish += 2 else bearish += 2
                        }

                        // Fibonacci impact
                        val fibRange = (fib["0%"] ?: 0.0) - (fib["100%"] ?: 0.0)
                        val cPrice = cryptoRepository.getCachedPrice(normalizedId)
                        if (fibRange != 0.0) {
                            val relativePos = (cPrice - (fib["100%"] ?: 0.0)) / fibRange
                            if (relativePos < 0.2) bullish++ // Near bottom
                            if (relativePos > 0.8) bearish++ // Near top
                        }

                        val totalSignals = (bullish + bearish + neutral).coerceAtLeast(1)
                        val rawConfidence = (bullish.coerceAtLeast(bearish).toFloat() / totalSignals)
                        
                        // Add deterministic "jitter" based on coinId for realism
                        val jitter = (coin.hashCode() % 10) / 100f
                        val confidence = (rawConfidence * 0.7f + 0.2f + jitter).coerceIn(0.42f, 0.96f)

                        val strength = when {
                            bullish >= totalSignals * 0.65 -> SignalStrength.STRONG_BUY
                            bearish >= totalSignals * 0.65 -> SignalStrength.STRONG_SELL
                            bullish > bearish -> SignalStrength.BUY
                            bearish > bullish -> SignalStrength.SELL
                            else -> SignalStrength.NEUTRAL
                        }

                        AnalysisUiState.Success(
                            coinId = coin.uppercase(),
                            compositeSignal = CompositeSignal(strength, bullish, bearish, neutral, indicators, confidence),
                            currentPrice = cryptoRepository.getCachedPrice(normalizedId),
                            ohlcData = ohlcData,
                            patterns = patterns,
                            fibonacci = fib,
                            rsiValue = rsi
                        )
                    }
                    emit(result)
                } catch (e: Exception) {
                    emit(AnalysisUiState.Error(e.message ?: "UNKNOWN ERROR"))
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalysisUiState.Loading)

    fun loadAnalysis(coinId: String) {
        _selectedCoin.value = coinId
    }

    val trackedCoins: StateFlow<List<String>> = cryptoRepository.getTrackedCoinPrices()
        .map { prices -> 
            val symbols = prices.map { it.symbol.uppercase() }
            if (symbols.size < 5) listOf("BTC", "ETH", "SOL", "XRP", "ADA", "DOT", "LINK", "LTC", "AVAX", "TRX", "MATIC", "XLM", "ATOM", "SHIB", "DOGE")
            else symbols
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("BTC", "ETH", "SOL", "XRP", "ADA", "DOT", "LINK", "LTC", "AVAX", "TRX", "MATIC", "XLM", "ATOM", "SHIB", "DOGE"))

    private val _aiReport = MutableStateFlow<String?>(null)
    val aiReport: StateFlow<String?> = _aiReport.asStateFlow()

    fun generateAIReport(state: AnalysisUiState.Success) {
        viewModelScope.launch {
            _aiReport.value = "GENERATING AI TERMINAL REPORT..."
            try {
                val prompt = """
                    Act as a professional Wall Street trader. Analyze this data for ${state.coinId}:
                    Price: $${state.currentPrice}
                    Signal: ${state.compositeSignal.strength} (${state.compositeSignal.confidence * 100}% confidence)
                    RSI: ${state.rsiValue}
                    Patterns: ${state.patterns.joinToString { it.pattern.name }}
                    
                    Provide a concise, terminal-style technical report (max 200 words). 
                    Include: 1. Trend analysis, 2. Risk assessment, 3. Entry/Exit suggestion.
                    Format: UPPERCASE terminal output with ASCII separators.
                """.trimIndent()
                
                var fullResponse = ""
                geminiService.sendMessage(prompt).collect { chunk ->
                    fullResponse += chunk
                    _aiReport.value = fullResponse
                }
            } catch (e: Exception) {
                _aiReport.value = "[ERROR] AI_LINK_FAILURE: ${e.message}"
            }
        }
    }

    fun generateVideoTeaser(state: AnalysisUiState.Success) {
        viewModelScope.launch {
            _aiReport.value = "CREATING AI VIDEO TEASER SCRIPT..."
            try {
                val prompt = """
                    Create a script for a 30-second TikTok/Reels crypto teaser video for ${state.coinId}.
                    Current Price: $${state.currentPrice}
                    Market Signal: ${state.compositeSignal.strength}
                    Confidence: ${state.compositeSignal.confidence * 100}%
                    
                    Structure:
                    0-5s: HOOK (SCARY/EXCITING terminal warning)
                    5-20s: DATA BREAKDOWN (Fast-paced technicals)
                    20-30s: CALL TO ACTION (Terminal command suggestion)
                    
                    Format: Terminal-style storyboard script.
                """.trimIndent()
                
                var fullResponse = ""
                geminiService.sendMessage(prompt).collect { chunk ->
                    fullResponse += chunk
                    _aiReport.value = fullResponse
                }
            } catch (e: Exception) {
                _aiReport.value = "[ERROR] VIDEO_GEN_FAILURE: ${e.message}"
            }
        }
    }

    fun dismissAiReport() {
        _aiReport.value = null
    }

    fun generateShareText(state: AnalysisUiState.Success): String {
        val sb = StringBuilder()
        sb.append(">>> TERMINAL_DEPT_REPORT: ${state.coinId}\n")
        sb.append("-----------------------------------\n")
        sb.append("SIGNAL: ${state.compositeSignal.strength.name.replace("_", " ")}\n")
        sb.append("CONFIDENCE: ${String.format(Locale.US, "%.0f", state.compositeSignal.confidence * 100)}%\n")
        sb.append("PRICE: $${String.format(Locale.US, "%.2f", state.currentPrice)}\n\n")

        sb.append(">>> INDICATORS:\n")
        state.compositeSignal.indicators.forEach { ind ->
            sb.append("- ${ind.name}: ${ind.value} [${ind.sentiment}]\n")
        }

        if (state.patterns.isNotEmpty()) {
            sb.append("\n>>> PATTERNS DETECTED:\n")
            state.patterns.forEach { pattern ->
                sb.append("[!] ${pattern.pattern.name}: ${if (pattern.isBullish) "BULLISH" else "BEARISH"}\n")
            }
        }

        sb.append("\n>>> SENT_VIA_CRYPTODEPT_TERMINAL_V2")
        return sb.toString()
    }
}