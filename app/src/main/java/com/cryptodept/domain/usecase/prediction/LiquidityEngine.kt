package com.cryptodept.domain.usecase.prediction

import com.cryptodept.domain.model.Direction
import com.cryptodept.domain.model.LiquidityInsight
import com.cryptodept.domain.model.ModelVote
import com.cryptodept.domain.model.PredictionModel
import com.cryptodept.domain.repository.DerivativesRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiquidityEngine @Inject constructor(
    private val derivativesRepository: DerivativesRepository
) {
    suspend fun analyze(symbol: String, currentPrice: Double): Pair<ModelVote, LiquidityInsight> = coroutineScope {
        val oiDeferred = async { derivativesRepository.getOpenInterest(symbol) }
        val fundingDeferred = async { derivativesRepository.getFundingRate(symbol) }
        val ratioDeferred = async { derivativesRepository.getLongShortRatio(symbol) }
        val liquidationsDeferred = async { derivativesRepository.getLiquidationData(symbol) }

        val oiResult = oiDeferred.await()
        val fundingResult = fundingDeferred.await()
        val ratioResult = ratioDeferred.await()
        val liquidations = liquidationsDeferred.await().getOrNull()

        val oi = oiResult.getOrNull()?.openInterestUsd ?: 0.0
        val oiChange = oiResult.getOrNull()?.openInterestChange24h ?: 0.0
        val funding = fundingResult.getOrNull()?.aggregatedRate ?: 0.0
        val longShortRatio = ratioResult.getOrNull()?.first?.let { long ->
            val short = ratioResult.getOrNull()?.second ?: 1.0
            long / (long + short)
        } ?: 0.5

        // Determine Bias
        val (direction, confidence) = when {
            funding > 0.03 && longShortRatio > 0.65 -> Direction.STRONG_DOWN to 0.85f // Overleveraged longs, high risk of squeeze down
            funding < -0.03 && longShortRatio < 0.35 -> Direction.STRONG_UP to 0.85f // Overleveraged shorts, high risk of squeeze up
            oiChange > 5.0 && funding > 0.01 -> Direction.DOWN to 0.60f // Rising OI with positive funding (longs aggressive)
            oiChange > 5.0 && funding < -0.01 -> Direction.UP to 0.60f // Rising OI with negative funding (shorts aggressive)
            else -> Direction.SIDEWAYS to 0.30f
        }

        val sentimentBias = when {
            funding > 0.02 -> "HEAVILY_LONG"
            funding < -0.02 -> "HEAVILY_SHORT"
            else -> "NEUTRAL_EQUILIBRIUM"
        }

        val insight = LiquidityInsight(
            openInterest = oi,
            openInterestChange24h = oiChange,
            fundingRate = funding,
            longShortRatio = longShortRatio,
            majorLiquidationLevels = liquidations?.heatmapLevels?.map { it.price } ?: emptyList(),
            sentimentBias = sentimentBias
        )

        val vote = ModelVote(
            model = PredictionModel.LIQUIDITY_ENGINE,
            direction = direction,
            targetPrice = if (direction == Direction.UP || direction == Direction.STRONG_UP) currentPrice * 1.03 else currentPrice * 0.97,
            confidence = confidence,
            weight = 0.25f,
            reasoning = generateReasoning(insight, direction)
        )

        vote to insight
    }

    private fun generateReasoning(insight: LiquidityInsight, direction: Direction): String {
        val oiStatus = if (insight.openInterestChange24h > 0) "increasing institutional exposure" else "capital outflow"
        val fundingStatus = if (insight.fundingRate > 0) "longs paying shorts (Bullish bias)" else "shorts paying longs (Bearish bias)"
        
        return buildString {
            append("Liquidity analysis detects $oiStatus with a 24h OI change of ${String.format(Locale.US, "%.1f", insight.openInterestChange24h)}%. ")
            append("Funding rate is ${String.format(Locale.US, "%.4f", insight.fundingRate)}%, indicating $fundingStatus. ")
            if (insight.longShortRatio > 0.6) {
                append("Retail sentiment is heavily skewed towards Longs (${(insight.longShortRatio * 100).toInt()}%), increasing the probability of a Long Squeeze. ")
            } else if (insight.longShortRatio < 0.4) {
                append("Retail sentiment is heavily skewed towards Shorts (${((1-insight.longShortRatio) * 100).toInt()}%), increasing the probability of a Short Squeeze. ")
            }
            append("Verdict: ${direction.name} based on order-flow imbalance.")
        }
    }
}
