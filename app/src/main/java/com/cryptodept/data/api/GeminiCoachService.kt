package com.cryptodept.data.api

import com.cryptodept.BuildConfig
import com.cryptodept.data.db.TradeJournalEntity
import com.cryptodept.domain.model.TradeSetup
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiCoachService @Inject constructor() {
    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash-latest",
        apiKey = BuildConfig.GEMINI_API_KEY,
        systemInstruction = content {
            text("""You are an expert crypto trading coach analyzing a trader's journal and market conditions.
            Respond ONLY in English. Be direct, specific, data-driven. Max 3 bullet points per insight.
            Focus on: risk management, psychological patterns, market timing, position sizing.
            Reference specific numbers from the data provided. Never give vague advice.""")
        }
    )
    
    suspend fun analyzeJournal(
        trades: List<TradeJournalEntity>,
        currentRiskScore: Int,
        currentMarket: String
    ): Flow<String> = model.generateContentStream(
        buildPrompt(trades, currentRiskScore, currentMarket)
    ).map { it.text ?: "" }
    
    private fun buildPrompt(trades: List<TradeJournalEntity>, risk: Int, market: String): String {
        val stats = """
        TRADER JOURNAL DATA:
        Total trades: ${trades.size}
        Win rate: ${trades.count { it.status == "CLOSED_WIN" }.toDouble() / trades.size.coerceAtLeast(1) * 100}%
        Avg R:R: ${trades.mapNotNull { it.riskRewardActual }.average()}
        Current Risk Score: $risk/100
        Market Context: $market
        Recent 5 trades: ${trades.takeLast(5).joinToString { "${it.symbol} ${it.direction} ${it.status} (${it.pnlPercent?.let { p -> "%.1f".format(p) }}%)" }}
        """
        return "$stats\n\nProvide 3 specific, actionable insights for this trader."
    }

    suspend fun sendMessage(prompt: String): Flow<String> = model.generateContentStream(prompt).map { it.text ?: "" }
}
