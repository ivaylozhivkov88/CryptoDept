package com.cryptodept.domain.usecase.prediction

import com.cryptodept.domain.model.Direction
import com.cryptodept.domain.model.ModelVote
import com.cryptodept.domain.model.PredictionModel
import javax.inject.Inject
import kotlin.math.abs

/**
 * Анализира фазите на пазара според логиката на Ричард Уайкоф.
 * Този модел е ключов за откриване на институционална активност чрез
 * Springs (капани за мечки) и Upthrusts (капани за бикове).
 */
class WyckoffPhaseDetector @Inject constructor() {

    /**
     * Прогнозира следващото движение на базата на ценово действие и обем.
     */
    fun predict(closes: List<Double>, volumes: List<Double>): ModelVote {
        // Изискваме поне 40 периода за установяване на нива на подкрепа и съпротива
        if (closes.size < 40 || volumes.size < 40) return neutralVote()

        val lastCloses = closes.takeLast(20)
        val lastVolumes = volumes.takeLast(20)
        val avgVolume = volumes.average()

        val currentPrice = closes.last()
        val support = lastCloses.minOrNull() ?: currentPrice
        val resistance = lastCloses.maxOrNull() ?: currentPrice

        // 1. Детекция на SPRING (Фаза на акумулация - Shakeout)
        val isSpring = detectSpring(lastCloses, lastVolumes, support, avgVolume)

        // 2. Детекция на UPTHRUST (Фаза на дистрибуция - Fakeout)
        val isUpthrust = detectUpthrust(lastCloses, lastVolumes, resistance, avgVolume)

        return when {
            isSpring -> ModelVote(
                model = PredictionModel.WYCKOFF_PHASE,
                direction = Direction.STRONG_UP,
                targetPrice = currentPrice + (resistance - support),
                confidence = 0.85f,
                weight = PredictionModel.WYCKOFF_PHASE.baseWeight
            )
            isUpthrust -> ModelVote(
                model = PredictionModel.WYCKOFF_PHASE,
                direction = Direction.STRONG_DOWN,
                targetPrice = currentPrice - (resistance - support),
                confidence = 0.85f,
                weight = PredictionModel.WYCKOFF_PHASE.baseWeight
            )
            else -> detectTrendPhase(lastCloses, currentPrice)
        }
    }

    /**
     * Търси фалшив пробив под подкрепата (Spring).
     */
    private fun detectSpring(closes: List<Double>, volumes: List<Double>, support: Double, avgVol: Double): Boolean {
        val last3 = closes.takeLast(3)
        val last3Vol = volumes.takeLast(3)

        // Условие: Цената е паднала под подкрепата в последните 3 свещи
        val brokeBelow = last3.any { it < support }
        // Условие: Текущата цена е успяла да се върне над подкрепата
        val recovered = closes.last() > support
        // Условие: Пробивът е бил с нисък обем (липса на интерес за продажба)
        val lowVolumeOnBreak = (last3Vol.minOrNull() ?: 0.0) < avgVol * 1.1

        return brokeBelow && recovered && lowVolumeOnBreak
    }

    /**
     * Търси фалшив пробив над съпротивата (Upthrust).
     */
    private fun detectUpthrust(closes: List<Double>, volumes: List<Double>, resistance: Double, avgVol: Double): Boolean {
        val last3 = closes.takeLast(3)
        val last3Vol = volumes.takeLast(3)

        // Условие: Цената е преминала над съпротивата
        val brokeAbove = last3.any { it > resistance }
        // Условие: Бързо връщане под съпротивата
        val droppedBelow = closes.last() < resistance
        // Условие: Висок обем при пробива (абсорбиране на търсенето)
        val highVolumeOnFakeout = (last3Vol.maxOrNull() ?: 0.0) > avgVol

        return brokeAbove && droppedBelow && highVolumeOnFakeout
    }

    /**
     * Ако няма специфична Wyckoff структура, определяме текущата тренд фаза.
     */
    private fun detectTrendPhase(closes: List<Double>, currentPrice: Double): ModelVote {
        val firstHalfAvg = closes.take(10).average()
        val secondHalfAvg = closes.takeLast(10).average()

        return when {
            secondHalfAvg > firstHalfAvg * 1.015 -> ModelVote(
                model = PredictionModel.WYCKOFF_PHASE,
                direction = Direction.UP,
                targetPrice = currentPrice * 1.04,
                confidence = 0.5f,
                weight = PredictionModel.WYCKOFF_PHASE.baseWeight
            )
            secondHalfAvg < firstHalfAvg * 0.985 -> ModelVote(
                model = PredictionModel.WYCKOFF_PHASE,
                direction = Direction.DOWN,
                targetPrice = currentPrice * 0.96,
                confidence = 0.5f,
                weight = PredictionModel.WYCKOFF_PHASE.baseWeight
            )
            else -> neutralVote()
        }
    }

    private fun neutralVote() = ModelVote(
        model = PredictionModel.WYCKOFF_PHASE,
        direction = Direction.SIDEWAYS,
        targetPrice = 0.0,
        confidence = 0f,
        weight = PredictionModel.WYCKOFF_PHASE.baseWeight
    )
}