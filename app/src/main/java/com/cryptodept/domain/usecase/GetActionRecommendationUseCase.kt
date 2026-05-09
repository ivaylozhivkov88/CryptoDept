package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.CoinPrice
import com.cryptodept.util.AppConstants
import javax.inject.Inject

class GetActionRecommendationUseCase @Inject constructor(
    private val sentimentAnalyzer: SentimentAnalyzer
) {
    data class Recommendation(val action: String, val explanation: String)

    suspend operator fun invoke(prices: List<CoinPrice>): Recommendation {
        val avgChange = if (prices.isNotEmpty()) prices.map { it.priceChangePercentage24h }.average() else 0.0
        val pulse = try {
            sentimentAnalyzer.calculatePulse("BTC")
        } catch (_: Exception) {
            AppConstants.TA.RSI_NEUTRAL.toInt()
        }

        return when {
            avgChange > 1.0 && pulse >= 65 -> Recommendation("BUY", "Momentum + Sentiment Bullish.")
            avgChange < -1.0 && pulse <= 35 -> Recommendation("SELL", "Momentum + Sentiment Bearish.")
            else -> Recommendation("WAIT", "Neutral market conditions.")
        }
    }
}
