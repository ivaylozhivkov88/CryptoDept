package com.cryptodept.domain.model

data class PricePrediction(
    val coinId: String,
    val currentPrice: Double,
    val priceChange24h: Double = 0.0, // Добавено за Content Generation
    val timestamp: Long,
    val prediction1h: PriceTarget,
    val prediction4h: PriceTarget,
    val prediction24h: PriceTarget,
    val prediction7d: PriceTarget,
    val ensembleConsensus: EnsembleConsensus,
    val priceDistribution: PriceDistribution,
    val mtfConsensus: MTFConsensus? = null, // Добавено
    val modelsAgreement: Float,
    val dataQuality: Float,
    val calculatedAt: Long = System.currentTimeMillis(),
)

data class PriceTarget(
    val low: Double,
    val mid: Double,
    val high: Double,
    val direction: Direction,
    val confidence: Float,
    val keyLevel: Double? = null,
)

data class EnsembleConsensus(
    val direction: Direction,
    val overallConfidence: Float,
    val modelVotes: Map<PredictionModel, ModelVote>,
    val agreementScore: Float,
    val dissenterModels: List<PredictionModel>,
)

data class ModelVote(
    val model: PredictionModel,
    val direction: Direction,
    val targetPrice: Double,
    val confidence: Float,
    val weight: Float,
    val reasoning: String = "", // ФИКС: Добавено поле за динамичния текст
)

data class PriceDistribution(
    val percentile10: Double,
    val percentile25: Double,
    val percentile50: Double,
    val percentile75: Double,
    val percentile90: Double,
    val expectedValue: Double,
    val standardDeviation: Double,
    val skewness: Double,
)

enum class Direction { STRONG_UP, UP, SIDEWAYS, DOWN, STRONG_DOWN }

enum class PredictionModel(
    val displayName: String,
    val baseWeight: Float,
) {
    LINEAR_REGRESSION("Linear Regression", 0.10f),
    FOURIER_CYCLES("Fourier Cycle Analysis", 0.15f),
    MONTE_CARLO("Monte Carlo Simulation", 0.20f),
    ELLIOTT_WAVE("Elliott Wave Theory", 0.15f),
    WYCKOFF_PHASE("Wyckoff Method", 0.15f),
    FRACTAL_ANALYSIS("Fractal Dimension", 0.10f),
    HURST_EXPONENT("Hurst Exponent", 0.15f),
}
