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

        val action = when {
            avgChange > 1.5 && pulse >= 70 -> "ACCUMULATE"
            avgChange < -1.5 && pulse <= 30 -> "LIQUIDATE"
            avgChange > 0.5 && pulse >= 55 -> "BUY_LITE"
            avgChange < -0.5 && pulse <= 45 -> "SELL_LITE"
            else -> "HOLD_STABLE"
        }

        val explanation = when (action) {
            "ACCUMULATE" -> "Strong bullish momentum confirmed across top assets. Sentiment signals institutional pressure; consider scaling into high-conviction positions."
            "LIQUIDATE" -> "Aggressive bearish breakdown in progress. Market sentiment reflects extreme panic; prioritize capital preservation and reduce exposure."
            "BUY_LITE" -> "Moderate recovery detected. Social pulse is improving, suggesting a potential short-term bounce for major currency pairs."
            "SELL_LITE" -> "Minor distribution phase active. Technical indicators suggest temporary exhaustion; watch for support retests before re-entry."
            else -> "Market is currently range-bound with neutral volatility. No clear edge detected; maintain existing positions and await clear volume confirmation."
        }

        return Recommendation(action, explanation)
    }
}
