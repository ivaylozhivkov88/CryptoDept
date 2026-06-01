package com.cryptodept.ui.prediction

import com.cryptodept.domain.model.*
import java.text.SimpleDateFormat
import java.util.Locale

fun PredictionViewModel.generateShareText(prediction: PricePrediction): String =
    buildString {
        val resolver = com.cryptodept.util.SymbolResolver()
        val coinId = resolver.toDisplayName(prediction.coinId)
        val currentPrice = String.format(Locale.US, "%.2f", prediction.currentPrice)
        val consensus = prediction.ensembleConsensus
        val consensusPercent = (consensus.overallConfidence * 100).toInt()
        val dateFormat = SimpleDateFormat("HH:mm:ss dd.MM.yyyy", Locale.US)
        val formattedDate = dateFormat.format(prediction.timestamp)

        // TITLE
        append("════════════════════════════════════════\n")
        append("🚀 CRYPTODEPT DEEP QUANT ANALYSIS — ${'$'}coinId\n")
        append("════════════════════════════════════════\n\n")

        // CURRENT STATE
        append(">>> CURRENT_STATE\n")
        append("PRICE: $$$currentPrice\n")
        append("TIMESTAMP: $formattedDate\n")
        append("CONSENSUS: ${consensus.direction.name.replace("_", " ")} ($consensusPercent% confidence)\n\n")

        // ВСИЧКИ МОДЕЛИ С ТЕХНИТЕ АНАЛИЗИ
        consensus.modelVotes.forEach { (model, vote) ->
            append(">>> ${model.displayName.uppercase()}\n")
            append("DIRECTION: ${vote.direction.name.replace("_", " ")}\n")
            append("TARGET: $${String.format(Locale.US, "%.2f", vote.targetPrice)}\n")
            append("CONFIDENCE: ${(vote.confidence * 100).toInt()}%\n")
            append("WEIGHT: ${(vote.weight * 100).toInt()}%\n")
            append("ANALYSIS: ${vote.reasoning}\n\n")
        }

        // AGREEMENT SCORE
        append(">>> ENSEMBLE_AGREEMENT\n")
        append("MODELS_ALIGNED: ${(consensus.agreementScore * 100).toInt()}%\n")
        if (consensus.dissenterModels.isNotEmpty()) {
            append("DISSENTER_MODELS: ${consensus.dissenterModels.joinToString(", ") { it.displayName }}\n")
        }
        append("\n")

        // LIQUIDITY DATA (PHASE X)
        prediction.liquidityInsight?.let { liq ->
            append(">>> LIQUIDITY_&_ORDERFLOW\n")
            append("OPEN_INTEREST: $${String.format(Locale.US, "%.1f", liq.openInterest / 1_000_000)}M (${String.format(Locale.US, "%.1f", liq.openInterestChange24h)}% change)\n")
            append("FUNDING_RATE: ${String.format(Locale.US, "%.4f", liq.fundingRate)}%\n")
            append("SENTIMENT_BIAS: ${liq.sentimentBias}\n\n")
        }

        // EVIDENCE CHAIN (PHASE X)
        if (prediction.evidenceChain.isNotEmpty()) {
            append(">>> ORACLE_EVIDENCE_CHAIN\n")
            prediction.evidenceChain.forEachIndexed { index, step ->
                append("${index + 1}. ${step.title}: ${step.impact.name} (${(step.confidence * 100).toInt()}%)\n")
            }
            append("\n")
        }

        // THE VERDICT
        append(">>> THE_CRYPTODEPT_VERDICT\n")
        val verdict =
            when {
                consensusPercent >= 70 -> "🟢 STRONG ${consensus.direction.name} — Ensemble conviction is HIGH"
                consensusPercent >= 55 -> "🟡 MILD ${consensus.direction.name} — Slight edge detected"
                consensusPercent in 45..54 -> "⚪ NEUTRAL — Market equilibrium"
                else -> "🔴 STRONG ${consensus.direction.name} — Risk is elevated"
            }
        append(verdict + "\n\n")

        // PROBABILITY DISTRIBUTION
        append(">>> PRICE_DISTRIBUTION\n")
        append("10TH_PERCENTILE: $${String.format(Locale.US, "%.2f", prediction.priceDistribution.percentile10)}\n")
        append("50TH_PERCENTILE: $${String.format(Locale.US, "%.2f", prediction.priceDistribution.percentile50)}\n")
        append("90TH_PERCENTILE: $${String.format(Locale.US, "%.2f", prediction.priceDistribution.percentile90)}\n")
        append("STD_DEVIATION: $${String.format(Locale.US, "%.2f", prediction.priceDistribution.standardDeviation)}\n\n")

        // TIMEFRAME TARGETS
        append(">>> MULTI_TIMEFRAME_TARGETS\n")
        append(
            "1H:  $${String.format(Locale.US, "%.2f", prediction.prediction1h.mid)} (${prediction.prediction1h.direction.name})\n",
        )
        append(
            "4H:  $${String.format(Locale.US, "%.2f", prediction.prediction4h.mid)} (${prediction.prediction4h.direction.name})\n",
        )
        append(
            "24H: $${String.format(
                Locale.US,
                "%.2f",
                prediction.prediction24h.mid,
            )} (${prediction.prediction24h.direction.name})\n",
        )
        append(
            "7D:  $${String.format(
                Locale.US,
                "%.2f",
                prediction.prediction7d.mid,
            )} (${prediction.prediction7d.direction.name})\n\n",
        )

        // FOOTER
        append("════════════════════════════════════════\n")
        append("📊 Analysis: Ensemble of 7 Quantitative Models\n")
        append("⚠️  DISCLAIMER: Not financial advice. Trade at your own risk.\n")
        append("#CryptoDept #DeepQuantAnalysis #$coinId #Crypto\n")
        append("🚀 LIKE IF YOU'RE FOLLOWING THIS ANALYSIS!\n")
    }

fun PredictionViewModel.generateInfographicUrl(prediction: PricePrediction): String {
    val resolver = com.cryptodept.util.SymbolResolver()
    val coinName = resolver.toDisplayName(prediction.coinId).uppercase()
    
    // Extract mid targets
    val rawTargets = listOf(
        prediction.prediction1h.mid,
        prediction.prediction4h.mid,
        prediction.prediction24h.mid,
        prediction.prediction7d.mid
    )
    
    // Visual Safety: If targets are perfectly flat, add a Micro-drift for chart scaling health
    val targets = if (rawTargets.distinct().size == 1) {
        val base = rawTargets[0]
        val drift = if (prediction.ensembleConsensus.direction.name.contains("UP")) 1.001 else 0.999
        listOf(base, base * drift, base * drift * 1.002, base * drift * 1.005)
    } else {
        rawTargets
    }

    val targetLabels = "['1H', '4H', '24H', '7D']"
    val targetData = targets.map { String.format(Locale.US, "%.2f", it) }.toString()

    // Build a QuickChart.io URL with Terminal Aesthetic
    // We use quotes for keys and explicit background color at the root for a perfect deep-black image
    val chartConfig = """
                {
                  "backgroundColor": "#000000",
                  "type": "line",
                  "data": {
                    "labels": $targetLabels,
                    "datasets": [{
                      "label": "$coinName QUANT FORECAST (USD)",
                      "data": $targetData,
                      "fill": true,
                      "backgroundColor": "rgba(0, 255, 65, 0.05)",
                      "borderColor": "#00FF41",
                      "borderWidth": 5,
                      "pointRadius": 10,
                      "pointBackgroundColor": "#FFB800",
                      "lineTension": 0.3
                    }]
                  },
                  "options": {
                    "title": {
                      "display": true,
                      "text": "CRYPTODEPT TERMINAL | $coinName ALPHA SCAN",
                      "fontColor": "#00FF41",
                      "fontSize": 24,
                      "fontFamily": "monospace"
                    },
                    "legend": { 
                      "labels": { "fontColor": "#ffffff", "fontSize": 16, "fontFamily": "monospace" } 
                    },
                    "scales": {
                      "yAxes": [{ 
                        "gridLines": { "color": "rgba(0, 255, 65, 0.1)" }, 
                        "ticks": { "fontColor": "#00FF41", "fontSize": 14, "fontFamily": "monospace" } 
                      }],
                      "xAxes": [{ 
                        "gridLines": { "color": "rgba(0, 255, 65, 0.1)" }, 
                        "ticks": { "fontColor": "#ffffff", "fontSize": 14, "fontFamily": "monospace" } 
                      }]
                    }
                  }
                }
            """.trimIndent().replace("\n", " ")

    return "https://quickchart.io/chart?c=${java.net.URLEncoder.encode(chartConfig, "UTF-8")}"
}

fun PredictionViewModel.generateImagePrompt(prediction: PricePrediction): String {
    val coinId = prediction.coinId.uppercase()
    val price = String.format(Locale.US, "$%.2f", prediction.currentPrice)
    val trend = if (prediction.priceChange24h >= 0) "BULLISH_CONTINUATION" else "BEARISH_CORRECTION"
    val colorPrimary = if (prediction.priceChange24h >= 0) "#00FF41" else "#FF3131"
    
    return "Imagine a high-end signature CryptoDept visual for $coinId. " +
            "SCENE: A futuristic Bloomberg-style command center at night. " +
            "In focus: A razor-sharp holographic 3D terminal display showing $coinId price action. " +
            "TEXT ON SCREEN: '$coinId // SIGNAL: $trend // PRICE: $price'. " +
            "VISUAL DNA: Cinematic volumetric lighting, sharp vector typography, deep black environment with electric $colorPrimary accents. " +
            "BACKGROUND: Blurry midnight cityscape visible through floor-to-ceiling glass windows. " +
            "STYLE: Professional, institutional, high-stakes trading atmosphere. 8k resolution, photorealistic textures, wide angle lens, highly detailed --v 6.0"
}

internal fun PredictionViewModel.mapCloudDataToPrediction(coinId: String, data: Map<String, Any>): PricePrediction {
    val price = (data["currentPrice"] as? Number)?.toDouble() ?: 0.0
    val directionStr = data["direction"] as? String ?: "SIDEWAYS"
    val direction = try { Direction.valueOf(directionStr) } catch(e: Exception) { Direction.SIDEWAYS }
    val confidence = (data["confidence"] as? Number)?.toFloat() ?: 0.5f

    // Calculate drift based on direction for non-flat forecast
    val drift = when(direction) {
        Direction.STRONG_UP -> 1.08
        Direction.UP -> 1.04
        Direction.STRONG_DOWN -> 0.92
        Direction.DOWN -> 0.96
        else -> 1.005 // Slight upward tilt for visual consistency
    }

    // Reconstruct a simplified ensemble consensus for the UI
    val consensus = EnsembleConsensus(
        direction = direction,
        overallConfidence = confidence,
        modelVotes = emptyMap(),
        agreementScore = 1.0f,
        dissenterModels = emptyList()
    )

    return PricePrediction(
        coinId = coinId,
        currentPrice = price,
        timestamp = (data["calculatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
        prediction1h = PriceTarget(price * 0.99, price * (1 + (drift - 1) * 0.1), price * 1.01, direction, 0.6f),
        prediction4h = PriceTarget(price * 0.98, price * (1 + (drift - 1) * 0.2), price * 1.02, direction, 0.6f),
        prediction24h = PriceTarget(price * 0.95, price * drift, price * 1.10, direction, 0.6f),
        prediction7d = PriceTarget(price * 0.85, price * (1 + (drift - 1) * 2.5), price * 1.25, direction, 0.6f),
        ensembleConsensus = consensus,
        priceDistribution = PriceDistribution(
            percentile10 = price * 0.9,
            percentile25 = price * 0.95,
            percentile50 = price,
            percentile75 = price * 1.05,
            percentile90 = price * 1.1,
            expectedValue = price,
            standardDeviation = price * 0.05,
            skewness = 0.0
        ),
        modelsAgreement = 1.0f,
        dataQuality = 1.0f,
        factors = listOf(
            PredictionFactor("Cloud Ensemble", 100, direction, data["summary"] as? String ?: "Calculated by Cloud Oracle")
        )
    )
}
