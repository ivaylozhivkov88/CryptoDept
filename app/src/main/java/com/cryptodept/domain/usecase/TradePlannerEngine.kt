package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TradePlannerEngine @Inject constructor(
    private val taEngine: TechnicalAnalysisEngine,
    private val riskEngine: RiskScoreEngine,
    private val mtfAnalyzer: MultiTimeframeAnalyzer,
    private val hapticManager: com.cryptodept.util.HapticManager
) {
    suspend fun evaluate(
        coin: String,
        direction: TradeDirectionType,
        entryPrice: Double,
        stopLoss: Double,
        takeProfit: Double,
        currentRsi: Double,
        fundingRate: Double,
        fearGreedIndex: Int,
        riskScore: Int
    ): TradeSetup = withContext(Dispatchers.Default) {

        val isLong = direction == TradeDirectionType.LONG
        val rrRatio = if (kotlin.math.abs(entryPrice - stopLoss) > 0) {
            kotlin.math.abs(takeProfit - entryPrice) / kotlin.math.abs(entryPrice - stopLoss)
        } else 0.0

        // MTF Consensus
        val mtf = try { mtfAnalyzer.analyze(coin) } catch (e: Exception) { null }
        val higherTFsBullish = mtf?.timeframes
            ?.filter { it.timeframe in listOf("4H", "1D", "1W") }
            ?.count { it.overallSignal in listOf(OverallSignal.BUY, OverallSignal.STRONG_BUY) } ?: 0

        val checklist = mutableListOf<ChecklistItem>()

        // TREND ALIGNMENT
        val dailySignal = mtf?.timeframes?.find { it.timeframe == "1D" }?.overallSignal
        val isDailyBullish = dailySignal == OverallSignal.BUY || dailySignal == OverallSignal.STRONG_BUY
        val isDailyBearish = dailySignal == OverallSignal.SELL || dailySignal == OverallSignal.STRONG_SELL

        checklist.add(ChecklistItem("TREND", "Higher timeframes (4H+) aligned with trade",
            if (isLong) higherTFsBullish >= 2 else higherTFsBullish <= 1, true))
        checklist.add(ChecklistItem("TREND", "Daily trend supports direction",
            if (isLong) isDailyBullish else isDailyBearish,
            false))

        // CONFLUENCE
        checklist.add(ChecklistItem("CONFLUENCE", "RSI supports entry",
            if (isLong) currentRsi < 50 else currentRsi > 50, false))
        checklist.add(ChecklistItem("CONFLUENCE", "Funding rate supports direction",
            if (isLong) fundingRate < 0.05 else fundingRate > 0.03, false))
        checklist.add(ChecklistItem("CONFLUENCE", "Fear & Greed contrarian signal",
            if (isLong) fearGreedIndex < 40 else fearGreedIndex > 65, false))

        // RISK MANAGEMENT
        checklist.add(ChecklistItem("RISK", "R:R ratio ≥ 2:1",
            rrRatio >= 2.0, true))
        checklist.add(ChecklistItem("RISK", "Stop loss defined and logical",
            stopLoss > 0 && if (entryPrice > 0) kotlin.math.abs(entryPrice - stopLoss) / entryPrice < 0.10 else false,
            true))
        checklist.add(ChecklistItem("RISK", "Take profit defined",
            takeProfit > 0, false))

        // MARKET CONDITIONS
        checklist.add(ChecklistItem("MARKET", "Overall Risk Score acceptable (<70)",
            riskScore < 70, false))
        checklist.add(ChecklistItem("MARKET", "Not trading against extreme sentiment",
            !(isLong && fearGreedIndex > 85) && !(!isLong && fearGreedIndex < 15),
            false))

        val passed = checklist.count { it.isPassed }
        val criticalFail = checklist.any { it.isCritical && !it.isPassed }

        val verdict = when {
            criticalFail -> SetupVerdict.AVOID
            passed >= 8  -> SetupVerdict.STRONG_SETUP
            passed >= 6  -> SetupVerdict.GOOD_SETUP
            passed >= 4  -> SetupVerdict.PROCEED_CAUTION
            else         -> SetupVerdict.AVOID
        }

        if (verdict == SetupVerdict.STRONG_SETUP) hapticManager.success()
        if (verdict == SetupVerdict.AVOID) hapticManager.error()

        TradeSetup(coin, direction, entryPrice, stopLoss, takeProfit,
            checklist, passed, checklist.size, verdict)
    }
}
