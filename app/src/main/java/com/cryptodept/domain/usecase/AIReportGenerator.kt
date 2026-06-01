package com.cryptodept.domain.usecase

import android.util.Log
import com.cryptodept.BuildConfig
import com.cryptodept.domain.model.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIReportGenerator
    @Inject
    constructor(
        private val remoteConfig: com.cryptodept.data.remoteconfig.RemoteConfigService,
        private val demoMode: com.cryptodept.util.DemoModeProvider,
    ) {
        private val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
        
        private fun getApiUrl(): String = "$GEMINI_BASE_URL${remoteConfig.getGeminiModel()}:generateContent"
        private fun getStreamUrl(): String = "$GEMINI_BASE_URL${remoteConfig.getGeminiModel()}:streamGenerateContent"

        companion object {
            private const val MAX_TOKENS = 2000
        }

        private val client =
            OkHttpClient
                .Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build()

        private val streamingClient = 
            client.newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS) // For streaming we want to keep it open
                .build()

        private val gson = Gson()

        suspend fun generateCoinAnalysis(
            coinName: String,
            coinSymbol: String,
            data: MarketDataSnapshot,
        ): Result<ParsedReport> =
            withContext(Dispatchers.IO) {
                try {
                    val prompt = buildCoinPrompt(coinName, coinSymbol, data)
                    val rawText = callGemini(prompt).getOrThrow()
                    Result.success(parseReport(rawText, data))
                } catch (e: Exception) {
                    Log.e("CryptoDept_AI", "Gemini coin analysis failed: ${e.message}")
                    Result.failure(e)
                }
            }

        suspend fun generateMarketAnalysis(data: MarketDataSnapshot): Result<ParsedReport> =
            withContext(Dispatchers.IO) {
                try {
                    val prompt = buildMarketPrompt(data)
                    val rawText = callGemini(prompt).getOrThrow()
                    Result.success(parseReport(rawText, data))
                } catch (e: Exception) {
                    Log.e("CryptoDept_AI", "Gemini market analysis failed: ${e.message}")
                    Result.failure(e)
                }
            }

        suspend fun generateShortSummary(data: MarketDataSnapshot): Result<String> =
            withContext(Dispatchers.IO) {
                try {
                    val prompt = buildShortSummaryPrompt(data)
                    callGemini(prompt)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        fun generateMarketAnalysisStream(data: MarketDataSnapshot): Flow<String> {
            val prompt = buildMarketPrompt(data)
            return callGeminiStream(prompt)
        }

        fun generateCoinAnalysisStream(
            coinName: String,
            coinSymbol: String,
            data: MarketDataSnapshot,
        ): Flow<String> {
            val prompt = buildCoinPrompt(coinName, coinSymbol, data)
            return callGeminiStream(prompt)
        }

        fun generateShortSummaryStream(agentId: String, data: MarketDataSnapshot): Flow<String> {
            if (demoMode.isActive()) {
                return flow { emit(demoMode.getDemoAiNarrative()) }
            }
            val prompt = when(agentId) {
                "AGENT-SENTINEL" -> buildSentinelPrompt(data)
                "AGENT-PULSE" -> buildPulsePrompt(data)
                "AGENT-QUANT" -> buildQuantPrompt(data)
                else -> buildShortSummaryPrompt(data)
            }
            return callGeminiStream(prompt)
        }

        private fun buildSentinelPrompt(data: MarketDataSnapshot): String = """
            Act as SENTINEL, the Technical Analysis Agent.
            DATA: BTC ${data.priceChange24h}%, RSI: ${data.rsi.toInt()}, Trend: ${data.ema50Signal}.
            TASK: 50-word technical SITREP. Focus on structural breakouts and momentum. 
            NOTE: Quantify uncertainty. Use probabilistic ranges instead of absolute claims.
            Tone: Professional, clinical, data-only. No emojis.
        """.trimIndent()

        private fun buildPulsePrompt(data: MarketDataSnapshot): String = """
            Act as PULSE, the Market Sentiment Agent.
            DATA: Fear & Greed Index: ${data.fearGreedIndex}/100, Risk Score: ${data.riskScore}/100.
            TASK: 50-word psychological SITREP. Focus on crowd behavior and social bias.
            NOTE: Quantify uncertainty. Express levels of confidence in sentiment shifts.
            Tone: Psychological, slightly cynical, objective. No emojis.
        """.trimIndent()

        private fun buildQuantPrompt(data: MarketDataSnapshot): String = """
            Act as QUANT, the Oracle Prediction Agent.
            DATA: BTC Price: $${data.price}, Volatility: ${data.priceChange24h}%, Liquidity Risk: ${data.riskScore}/100.
            TASK: 50-word strategic SITREP. Focus on price targets and probability distribution.
            NOTE: Quantify uncertainty. Always provide probabilistic intervals for targets.
            Tone: Strategic, forward-looking, quantitative. No emojis.
        """.trimIndent()

        private fun buildShortSummaryPrompt(data: MarketDataSnapshot): String =
            """
            Act as a Quantitative Market Analyst. 
            Synthesize a concise intelligence report for the global cryptocurrency market. 
            Analyze the following data points and provide a professional, data-driven summary.
            
            [MARKET_DATA]
            BTC 24h Change: ${data.priceChange24h}%
            BTC Dominance: ${data.btcDominance}%
            Sentiment (Fear & Greed): ${data.fearGreedIndex}/100
            Systemic Risk: ${data.riskScore}/100
            Macro: S&P ${data.sp500Change}%, DXY ${data.dxyChange}%
            
            [TECHNICAL_STATUS]
            - RSI: ${data.rsi.toInt()}
            - Trend Bias: ${data.ema50Signal}
            - Risk Score: ${data.riskScore}/100

            Structure your report EXACTLY as follows:
            >>> MARKET_STATUS_SUMMARY
            VERDICT: [1-3 word high-level verdict, e.g., ACCUMULATION, DISTRIBUTION, CONSOLIDATION]
            
            ANALYSIS: 
            [Provide a data-driven analysis in 2 concise paragraphs. Discuss the relationship between technical momentum, liquidity, and sentiment.]
            
            METRICS:
            - MOMENTUM: [Technical flag]
            - LIQUIDITY: [Whale flow / Order-flow flag]
            - SENTIMENT: [Psychological state flag]
            
            STRATEGIC_BIAS: [NEUTRAL / BULLISH / BEARISH]
            
            Tone: Professional, clinical, objective. NO emojis.
            Target length: 100-150 words.
            """.trimIndent()

        private suspend fun callGemini(prompt: String): Result<String> {
            // ... (keep existing callGemini for non-streaming usage)
            return try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isBlank()) {
                    return Result.failure(IllegalStateException("GEMINI_API_KEY is missing"))
                }

                val requestBody =
                    mapOf(
                        "contents" to
                            listOf(
                                mapOf("parts" to listOf(mapOf("text" to prompt))),
                            ),
                        "generationConfig" to
                            mapOf(
                                "maxOutputTokens" to MAX_TOKENS,
                                "temperature" to 0.7,
                            ),
                    )

                val request =
                    Request
                        .Builder()
                        .url("${getApiUrl()}?key=$apiKey")
                        .post(gson.toJson(requestBody).toRequestBody("application/json".toMediaType()))
                        .build()

                // Use a capped call to avoid long hangs
                val response = client.newCall(request).execute()
                val bodyString = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    return Result.failure(Exception("Gemini API error: ${response.code}"))
                }

                val json = JSONObject(bodyString)
                val text =
                    json
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")

                Result.success(text)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        private fun callGeminiStream(prompt: String): Flow<String> = flow {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isBlank()) {
                    Log.e("CryptoDept_AI", "GEMINI_API_KEY is missing")
                    return@flow
                }

                val requestBody = mapOf(
                    "contents" to listOf(
                        mapOf("parts" to listOf(mapOf("text" to prompt)))
                    ),
                    "generationConfig" to mapOf(
                        "maxOutputTokens" to MAX_TOKENS,
                        "temperature" to 0.7
                    )
                )

                val request = Request.Builder()
                    .url("${getStreamUrl()}?alt=sse&key=$apiKey")
                    .post(gson.toJson(requestBody).toRequestBody("application/json".toMediaType()))
                    .build()

                // Use the dedicated streaming client with specific timeouts
                streamingClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("CryptoDept_AI", "Gemini stream API error: ${response.code}")
                        return@flow
                    }

                    val reader = response.body?.charStream()?.buffered() ?: return@flow
                    reader.useLines { lines ->
                        lines.forEach { line ->
                            if (line.startsWith("data: ")) {
                                val jsonChunk = line.removePrefix("data: ").trim()
                                if (jsonChunk.isNotEmpty() && jsonChunk != "[DONE]") {
                                    try {
                                        val json = JSONObject(jsonChunk)
                                        val candidates = json.optJSONArray("candidates") ?: return@forEach
                                        if (candidates.length() == 0) return@forEach

                                        val content = candidates.getJSONObject(0).optJSONObject("content") ?: return@forEach
                                        val parts = content.optJSONArray("parts") ?: return@forEach
                                        if (parts.length() == 0) return@forEach

                                        val text = parts.getJSONObject(0).optString("text", "")
                                        if (text.isNotEmpty()) {
                                            emit(text)
                                        }
                                    } catch (e: Exception) {
                                        Log.w("CryptoDept_AI", "Failed to parse SSE chunk: $e")
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CryptoDept_AI", "Gemini stream fatal error: ${e.message}")
            }
        }.flowOn(Dispatchers.IO)

        private fun buildCoinPrompt(
            coinName: String,
            symbol: String,
            d: MarketDataSnapshot,
        ): String =
            """
            Act as a Quantitative Analyst.
            Analyze $coinName ($symbol) based on provided data points.
            DATA: Price $${d.price}, RSI ${d.rsi}, MACD ${d.macdSignal}, EMA50 ${d.ema50Signal}, EMA200 ${d.ema200Signal}, BB ${d.bollingerPosition}.
            DERIVATIVES: Funding ${d.fundingRate} (${d.fundingLevel}), Liqs: L $${d.longLiquidations24h} / S $${d.shortLiquidations24h}.
            SENTIMENT: Fear&Greed ${d.fearGreedIndex}, News ${d.newsSentiment}.
            STRUCTURE: Wyckoff ${d.wyckoffPhase}, Elliott ${d.elliottWave}.
            RISK: ${d.riskScore}/100.

            Structure your response EXACTLY like this:
            **TLDR**
            [Summary]
            **Verdict:** [Bullish/Neutral/Bearish]
            **1. [Technical Analysis]**
            Overview: [Text]
            Implication: [Text]
            Key Levels: [Text]
            **2. [Market Dynamics]**
            Overview: [Text]
            Implication: [Text]
            Key Levels: [Text]
            **3. Strategic Outlook**
            Overview: [Text]
            Implication: [Text]
            Key Levels: [Text]
            **Conclusion**
            Outlook: **[Label]** [Final text]
            Primary Trigger: [Single trigger]
            """.trimIndent()

        private fun buildMarketPrompt(d: MarketDataSnapshot): String =
            """
            Act as a Quantitative Strategist.
            Analyze aggregate CRYPTO MARKET data.
            BTC Change: ${d.priceChange24h}%, Dominance: ${d.btcDominance}%, Fear&Greed: ${d.fearGreedIndex}.
            Funding: ${d.fundingRate}, Risk: ${d.riskScore}.
            Macro: S&P ${d.sp500Change}%, DXY ${d.dxyChange}%.

            Structure your response EXACTLY like this:
            **TLDR**
            [Summary]
            **Verdict:** [Bullish/Neutral/Bearish]
            **1. [Bitcoin Dominance & Trend]**
            Overview: [Text]
            Implication: [Text]
            Watch For: [Text]
            **2. [Liquidity & Derivatives]**
            Overview: [Text]
            Implication: [Text]
            Watch For: [Text]
            **3. [Macro Correlation]**
            Overview: [Text]
            Implication: [Text]
            Watch For: [Text]
            **4. Market Outlook**
            Overview: [Text]
            Implication: [Text]
            Watch For: [Text]
            **Conclusion**
            Outlook: **[Label]** [Final text]
            Key Indicator: [Single trigger]
            """.trimIndent()

        private fun parseReport(
            rawText: String,
            data: MarketDataSnapshot,
        ): ParsedReport {
            val tldr = extractSection(rawText, "**TLDR**", "**Verdict") ?: rawText.take(200)
            val verdictLine = rawText.lines().firstOrNull { it.contains("Verdict:", true) } ?: ""
            val verdict =
                when {
                    verdictLine.contains("Strong Bullish", true) -> ReportVerdict.STRONG_BULLISH
                    verdictLine.contains("Bullish", true) -> ReportVerdict.BULLISH
                    verdictLine.contains("Strong Bearish", true) -> ReportVerdict.STRONG_BEARISH
                    verdictLine.contains("Bearish", true) -> ReportVerdict.BEARISH
                    else -> ReportVerdict.NEUTRAL
                }

            val sections = mutableListOf<ReportSection>()
            val sectionPattern = Regex("""(?m)^\*\*\d+\. (.+?)\*\*\n([\s\S]+?)(?=^\*\*\d+\.|\*\*Conclusion|\Z)""")
            sectionPattern.findAll(rawText).forEach { match ->
                val body = match.groupValues[2].trim()
                sections.add(
                    ReportSection(
                        title = match.groupValues[1],
                        overview = extractLabel(body, "Overview:"),
                        whatItMeans = extractLabel(body, "What it means:"),
                        watchFor = extractLabel(body, "Watch for:"),
                    ),
                )
            }

            return ParsedReport(
                tldr = tldr.trim(),
                verdict = verdict,
                sections = sections,
                keyLevels = emptyList(),
                watchFor =
                    rawText
                        .lines()
                        .filter { it.contains("Watch for:", true) }
                        .map { it.substringAfter(":").trim() },
                disclaimer = "AI-generated analysis. Not financial advice.",
            )
        }

        private fun extractSection(
            text: String,
            start: String,
            end: String,
        ): String? {
            val s = text.indexOf(start).takeIf { it != -1 } ?: return null
            val e = text.indexOf(end, s + start.length).takeIf { it != -1 } ?: text.length
            return text.substring(s + start.length, e).trim()
        }

        private fun extractLabel(
            text: String,
            label: String,
        ): String {
            val s = text.indexOf(label, ignoreCase = true).takeIf { it != -1 } ?: return ""
            val next =
                listOf("Overview:", "What it means:", "Watch for:")
                    .mapNotNull {
                        val pos = text.indexOf(it, s + label.length)
                        if (pos != -1) pos else null
                    }.minOrNull() ?: text.length
            return text.substring(s + label.length, next).trim()
        }
    }
