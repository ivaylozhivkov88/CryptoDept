package com.cryptodept.domain.usecase

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
    }
