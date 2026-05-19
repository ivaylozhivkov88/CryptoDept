package com.cryptodept.data.api

import com.cryptodept.BuildConfig
import com.cryptodept.domain.model.TradeJournal
import com.cryptodept.domain.repository.AIProvider
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiCoachService
    @Inject
    constructor(
        private val remoteConfig: com.cryptodept.data.remoteconfig.RemoteConfigService
    ) : AIProvider {
        
        private fun createModel(apiKey: String) = GenerativeModel(
            modelName = remoteConfig.getGeminiModel(),
            apiKey = apiKey,
            systemInstruction = content {
                text(
                    """You are an expert crypto trading coach analyzing a trader's journal and market conditions.
    Respond ONLY in English. Be direct, specific, data-driven. Max 3 bullet points per insight.
    Focus on: risk management, psychological patterns, market timing, position sizing.
    Reference specific numbers from the data provided. Never give vague advice."""
                )
            }
        )

        private val primaryModel by lazy { createModel(BuildConfig.GEMINI_API_KEY) }
        private val secondaryModel by lazy { createModel(BuildConfig.GEMINI_API_KEY_ALT) }

        override suspend fun analyzeJournal(
            trades: List<TradeJournal>,
            currentRiskScore: Int,
            currentMarket: String,
        ): Flow<String> = flow {
            try {
                val response = primaryModel.generateContentStream(buildPrompt(trades, currentRiskScore, currentMarket))
                emitAll(response.map { it.text ?: "" })
            } catch (e: Exception) {
                try {
                    val response = secondaryModel.generateContentStream(buildPrompt(trades, currentRiskScore, currentMarket))
                    emitAll(response.map { it.text ?: "" })
                } catch (e2: Exception) {
                    throw e2 // Re-throw the last exception so Router can catch it
                }
            }
        }

        private fun buildPrompt(
            trades: List<TradeJournal>,
            risk: Int,
            market: String,
        ): String {
            val stats = """
        TRADER JOURNAL DATA:
        Total trades: ${trades.size}
        Win rate: ${trades.count { it.status.name == "CLOSED_WIN" }.toDouble() / trades.size.coerceAtLeast(1) * 100}%
        Avg R:R: ${trades.mapNotNull { it.riskRewardActual }.average()}
        Current Risk Score: $risk/100
        Market Context: $market
        Recent 5 trades: ${trades.takeLast(5).joinToString {
                "${it.symbol} ${it.direction.name} ${it.status.name} (${it.pnlPercent?.let { p ->
                    "%.1f".format(p)
                }}%)"
            }}
        """
            return "$stats\n\nProvide 3 specific, actionable insights for this trader."
        }

        override suspend fun sendMessage(prompt: String): Flow<String> = flow {
            try {
                val response = primaryModel.generateContentStream(prompt)
                emitAll(response.map { it.text ?: "" })
            } catch (e: Exception) {
                try {
                    val response = secondaryModel.generateContentStream(prompt)
                    emitAll(response.map { it.text ?: "" })
                } catch (e2: Exception) {
                    throw e2 // Re-throw the last exception so Router can catch it
                }
            }
        }
    }
