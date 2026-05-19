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

        fun generateShortSummaryStream(data: MarketDataSnapshot): Flow<String> {
            if (demoMode.isActive()) {
                return flow {
                    emit(demoMode.getDemoAiNarrative())
                }
            }
            val prompt = buildShortSummaryPrompt(data)
            return callGeminiStream(prompt)
        }

        private fun buildShortSummaryPrompt(data: MarketDataSnapshot): String =
            """
            Act as the CryptoDept ELITE AI Reasoning Orchestrator. 
            Synthesize a high-density intelligence report for the global crypto market. 
            Analyze the following sub-agent inputs and provide a professional, data-driven narrative.
            
            [DATA_STREAM]
            BTC Price Change: ${data.priceChange24h}%
            BTC Dominance: ${data.btcDominance}%
            Sentiment Index (Fear & Greed): ${data.fearGreedIndex}/100
            Systemic Risk Score: ${data.riskScore}/100
            Macro Context: S&P ${data.sp500Change}%, DXY ${data.dxyChange}%
            
            [AGENT_LOGS]
            TECHNICAL_SENTINEL: Identifying momentum structures based on BTC ${data.priceChange24h}% performance. RSI currently at ${data.rsi.toInt()}.
            GHOST_WHALE: Monitoring funding rates and institutional capital flow. Risk at ${data.riskScore}/100.
            SENTIMENT_PULSE: Tracking social dominance and euphoria/panic levels. F&G at ${data.fearGreedIndex}.

            Structure your report as a professional terminal briefing EXACTLY as follows:
            >>> MARKET_INTELLIGENCE_SUMMARY
            VERDICT: [1-3 word high-level verdict, e.g., ACCUMULATION, DISTRIBUTION, CONSOLIDATION]
            
            ANALYSIS: 
            [Provide a deep-dive analysis (2-3 detailed paragraphs). Discuss the confluence between technical momentum, whale flow, and sentiment. Mention BTC specifically.]
            
            AGENT_REPORTS:
            - SENTINEL: [Detailed technical flag]
            - SCOUT: [Whale flow / Liquidity flag]
            - PULSE: [Psychological state / Macro flag]
            
            BIAS: [AGGRESSIVE / NEUTRAL / DEFENSIVE] based on confluence.
            
            Tone: Professional, clinical, cynical, data-driven. NO emojis.
            Target length: 150-250 words.
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
            Act as a cynical hedge fund manager who hates hype and looks only at hard data.
            Analyze $coinName ($symbol).
            DATA: Price $${d.price}, RSI ${d.rsi}, MACD ${d.macdSignal}, EMA50 ${d.ema50Signal}, EMA200 ${d.ema200Signal}, BB ${d.bollingerPosition}.
            DERIVATIVES: Funding ${d.fundingRate} (${d.fundingLevel}), Liqs: L $${d.longLiquidations24h} / S $${d.shortLiquidations24h}.
            SENTIMENT: Fear&Greed ${d.fearGreedIndex}, News ${d.newsSentiment}.
            SMART MONEY: Wyckoff ${d.wyckoffPhase}, Elliott ${d.elliottWave}.
            RISK: Score ${d.riskScore}/100.

            Structure your response EXACTLY like this:
            **TLDR**
            [Summary]
            **Verdict:** [Strong Bullish/Bullish/Neutral/Bearish/Strong Bearish]
            **1. [Technical Title]**
            Overview: [Text]
            What it means: [Text]
            Watch for: [Text]
            **2. [Market Dynamics]**
            Overview: [Text]
            What it means: [Text]
            Watch for: [Text]
            **3. Near-term Market Outlook**
            Overview: [Text]
            What it means: [Text]
            Watch for: [Text]
            **Conclusion**
            Market Outlook: **[Label]** [Final text]
            Key watch: [Single trigger]
            """.trimIndent()

        private fun buildMarketPrompt(d: MarketDataSnapshot): String =
            """
            Act as a cynical hedge fund manager who hates hype and looks only at hard data.
            Analyze OVERALL CRYPTO MARKET.
            BTC Change: ${d.priceChange24h}%, Dominance: ${d.btcDominance}%, Fear&Greed: ${d.fearGreedIndex}.
            Funding: ${d.fundingRate}, Wyckoff: ${d.wyckoffPhase}, Risk: ${d.riskScore}.
            Macro: S&P ${d.sp500Change}%, DXY ${d.dxyChange}%.

            Structure your response EXACTLY like this:
            **TLDR**
            [Summary]
            **Verdict:** [Strong Bullish/Bullish/Neutral/Bearish/Strong Bearish]
            **1. [Bitcoin's Role]**
            Overview: [Text]
            What it means: [Text]
            Watch for: [Text]
            **2. [Derivatives Landscape]**
            Overview: [Text]
            What it means: [Text]
            Watch for: [Text]
            **3. [Macro Forces]**
            Overview: [Text]
            What it means: [Text]
            Watch for: [Text]
            **4. Near-term Market Outlook**
            Overview: [Text]
            What it means: [Text]
            Watch for: [Text]
            **Conclusion**
            Market Outlook: **[Label]** [Final text]
            Key watch: [Single trigger]
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
