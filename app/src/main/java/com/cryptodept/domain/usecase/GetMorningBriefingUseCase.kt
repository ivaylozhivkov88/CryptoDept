package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.MarketDataSnapshot
import com.cryptodept.domain.usecase.prediction.DailyAIPick
import com.cryptodept.domain.usecase.prediction.GetDailyAIPickUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class MorningBriefing(
    val narrative: String,
    val dailyPick: DailyAIPick?,
    val timestamp: Long = System.currentTimeMillis()
)

class GetMorningBriefingUseCase @Inject constructor(
    private val aiGenerator: AIReportGenerator,
    private val getDailyAIPickUseCase: GetDailyAIPickUseCase,
    private val repository: com.cryptodept.domain.repository.CryptoRepository,
    private val macroUseCase: GetMacroIntelligenceUseCase
) {
    suspend fun execute(): MorningBriefing {
        val dailyPick = getDailyAIPickUseCase.execute()
        val macro = macroUseCase().getOrNull()
        val prices = repository.getTrackedCoinPrices().first()
        val btc = prices.find { it.id == "bitcoin" }
        
        val snapshot = MarketDataSnapshot(
            price = btc?.currentPrice ?: 0.0,
            rsi = 50.0, // Placeholder
            macdSignal = "N/A",
            ema50Signal = "N/A",
            ema200Signal = "N/A",
            bollingerPosition = "N/A",
            fundingRate = 0.0,
            fundingLevel = "N/A",
            longLiquidations24h = macro?.totalLiquidations24h?.longsUsd ?: 0.0,
            shortLiquidations24h = macro?.totalLiquidations24h?.shortsUsd ?: 0.0,
            fearGreedIndex = 50,
            newsSentiment = "NEUTRAL",
            wyckoffPhase = "N/A",
            elliottWave = "N/A",
            riskScore = 50,
            priceChange24h = btc?.priceChangePercentage24h ?: 0.0,
            btcDominance = macro?.btcDominance ?: 50.0,
            sp500Change = 0.0,
            dxyChange = 0.0
        )
        
        val narrative = aiGenerator.generateShortSummary(snapshot).getOrDefault("MORNING_BRIEFING_OFFLINE: Node synchronization error.")
        
        return MorningBriefing(
            narrative = narrative,
            dailyPick = dailyPick
        )
    }
}
