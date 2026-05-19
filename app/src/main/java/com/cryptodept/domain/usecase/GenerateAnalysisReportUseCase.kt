package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.*
import com.cryptodept.domain.repository.AIProvider
import kotlinx.coroutines.flow.Flow
import java.util.Locale
import javax.inject.Inject

class GenerateAnalysisReportUseCase
    @Inject
    constructor(
        private val aiProvider: AIProvider,
    ) {
        suspend fun execute(result: DeepAnalysisResult): Flow<String> {
            val priceStr = String.format(Locale.US, "%.2f", result.currentPrice)
            val rsiStr = String.format(Locale.US, "%.1f", result.rsiValue)
            val confidenceInt = (result.compositeSignal.confidence * 100).toInt()
            val signalStr =
                result.compositeSignal.strength.name
                    .replace("_", " ")

            val prompt =
                "Generate a viral technical analysis report for ${result.coinId}.\n" +
                    "Price: $$priceStr, Signal: $signalStr, Confidence: $confidenceInt%, RSI: $rsiStr."

            return aiProvider.sendMessage(prompt)
        }

        /**
         * Generates an ELITE narrative report based on the full Oracle Ensemble data.
         * Includes model reasoning, liquidity, and confidence metrics.
         */
        suspend fun execute(
            prediction: PricePrediction,
            metrics: com.cryptodept.domain.prediction.ConfidenceMetrics?,
        ): Flow<String> {
            val resolver = com.cryptodept.util.SymbolResolver()
            val coin = resolver.toDisplayName(prediction.coinId)
            val price = String.format(Locale.US, "%.2f", prediction.currentPrice)
            val change = String.format(Locale.US, "%.2f", prediction.priceChange24h)
            val consensus = prediction.ensembleConsensus
            val conviction = (consensus.overallConfidence * 100).toInt()

            val prompt = buildString {
                append("Act as a Lead Quantitative Strategist for the 'CryptoDept Elite' financial terminal.\n")
                append("Generate a high-impact, professional narrative report for $coin.\n\n")

                append(">>> CORE_MARKET_DATA\n")
                append("PRICE: $$price (24H_CHANGE: $change%)\n")
                append("ORACLE_CONSENSUS: ${consensus.direction} (CONVICTION: $conviction%)\n")
                append("MODELS_AGREEMENT: ${(prediction.modelsAgreement * 100).toInt()}%\n")
                append("DATA_QUALITY: ${(prediction.dataQuality * 100).toInt()}%\n\n")

                if (metrics != null) {
                    append(">>> CONFIDENCE_METRICS\n")
                    append("AGREEMENT: ${metrics.modelAgreement}\n")
                    append("QUALITY: ${metrics.dataQuality}\n")
                    append("VOLATILITY: ${metrics.volatilityWarning}\n\n")
                }

                append(">>> ENSEMBLE_MODEL_REASONING\n")
                consensus.modelVotes.forEach { (model, vote) ->
                    append("- ${model.displayName}: ${vote.direction} (Conf: ${(vote.confidence * 100).toInt()}%). Reasoning: ${vote.reasoning}\n")
                }
                append("\n")

                prediction.liquidityInsight?.let {
                    append(">>> LIQUIDITY_&_ORDERFLOW\n")
                    append("OPEN_INTEREST: ${it.openInterest} (${it.openInterestChange24h}% change)\n")
                    append("FUNDING_RATE: ${it.fundingRate}%\n")
                    append("LONG/SHORT_RATIO: ${it.longShortRatio}\n")
                    append("SENTIMENT_BIAS: ${it.sentimentBias}\n\n")
                }

                if (prediction.evidenceChain.isNotEmpty()) {
                    append(">>> CHAIN_OF_EVIDENCE\n")
                    prediction.evidenceChain.forEach { step ->
                        append("- ${step.title}: ${step.impact} (Conf: ${(step.confidence * 100).toInt()}%)\n")
                    }
                    append("\n")
                }

                append(">>> TARGETS_&_PROBABILITY\n")
                append("FLOOR_SUPPORT: $${String.format(Locale.US, "%.2f", prediction.priceDistribution.percentile10)}\n")
                append("MEDIAN_PRICE: $${String.format(Locale.US, "%.2f", prediction.priceDistribution.percentile50)}\n")
                append("QUANT_TARGET: $${String.format(Locale.US, "%.2f", prediction.priceDistribution.percentile90)}\n\n")

                prediction.mtfConsensus?.let { mtf ->
                    append(">>> MULTI_TIMEFRAME_CONFLUENCE\n")
                    mtf.timeframes.forEach { tf ->
                        append("${tf.timeframe}: ${tf.overallSignal} (RSI: ${String.format(Locale.US, "%.1f", tf.rsi)})\n")
                    }
                    append("\n")
                }

                append("INSTRUCTIONS:\n")
                append("1. Be professional, data-driven, and slightly cynical. Use terminal-style language.\n")
                append("2. Synthesize the conflicting or agreeing model votes into a coherent strategic outlook.\n")
                append("3. Mention specific liquidity levels if available.\n")
                append("4. End with a one-sentence 'EXECUTIVE VERDICT'.\n")
                append("5. DO NOT include these instructions or [PROXY] headers in the response. Return ONLY the report content.\n")
                append("6. DO NOT use emojis. DO NOT provide financial advice.\n")
            }

            return aiProvider.sendMessage(prompt)
        }
    }
